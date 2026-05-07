package com.spring.ai.opencv.service.impl;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.opencv.config.FaceProperties;
import com.spring.ai.opencv.domain.request.FaceLoginVerifyRequest;
import com.spring.ai.opencv.domain.response.FaceLoginVerifyResponse;
import com.spring.ai.opencv.service.FaceRecognitionService;
import jakarta.annotation.Resource;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Real face recognition implementation based on YuNet + SFace.
 */
@Service
@ConditionalOnClass(name = {"org.opencv.core.Mat", "nu.pattern.OpenCV"})
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    private static final int FACE_ROW_COLUMNS = 15;
    private static final int FACE_BOX_X_INDEX = 0;
    private static final int FACE_BOX_Y_INDEX = 1;
    private static final int FACE_BOX_WIDTH_INDEX = 2;
    private static final int FACE_BOX_HEIGHT_INDEX = 3;
    private static final int FACE_SCORE_INDEX = 14;
    private static final double MIN_AREA_RATIO = 0.08D;

    @Resource
    private FaceProperties faceProperties;

    @Override
    public FaceLoginVerifyResponse verifyFace(FaceLoginVerifyRequest request) {
        if (request == null || !StringUtils.hasText(request.getImageBase64())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "Image content cannot be empty");
        }
        ensureReady();
        BufferedImage image = decodeImage(request.getImageBase64());
        BufferedImage normalizedImage = normalizeImageSize(image);
        Mat sourceMat = bufferedImageToMat(normalizedImage);

        try {
            Mat detections = detectFaces(sourceMat);
            int faceCount = detections.rows();
            if (faceCount <= 0) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "No face detected");
            }
            if (faceCount > 1) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "Please keep exactly one face in the image");
            }

            Mat bestFace = detections.row(0);
            double qualityScore = computeQualityScore(sourceMat, bestFace);
            if (qualityScore < faceProperties.getQualityThreshold()) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "Face image quality is too low");
            }

            double livenessScore = computeLivenessScore(sourceMat, bestFace);
            if (Boolean.TRUE.equals(faceProperties.getEnableLivenessCheck())
                    && livenessScore < faceProperties.getLivenessThreshold()) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "Liveness check failed");
            }

            String embedding = extractFaceEmbedding(sourceMat, bestFace);
            return FaceLoginVerifyResponse.builder()
                    .faceDetected(true)
                    .faceCount(faceCount)
                    .qualityScore(qualityScore)
                    .livenessScore(livenessScore)
                    .faceEmbedding(embedding)
                    .build();
        } finally {
            sourceMat.release();
        }
    }

    @Override
    public boolean isSameFace(String sourceEmbedding, String targetEmbedding) {
        float[] sourceVector = decodeEmbedding(sourceEmbedding);
        float[] targetVector = decodeEmbedding(targetEmbedding);
        if (sourceVector.length != targetVector.length || sourceVector.length == 0) {
            return false;
        }
        return cosineSimilarity(sourceVector, targetVector) >= faceProperties.getMatchThreshold();
    }

    @Override
    public int resolveEmbeddingDimension(String embedding) {
        return decodeEmbedding(embedding).length;
    }

    private Mat detectFaces(Mat image) {
        FaceDetectorYN detector = FaceDetectorYN.create(
                faceProperties.getDetectorModelPath(),
                "",
                new Size(image.cols(), image.rows()),
                faceProperties.getScoreThreshold(),
                faceProperties.getNmsThreshold(),
                faceProperties.getTopK()
        );
        detector.setInputSize(new Size(image.cols(), image.rows()));
        Mat detections = new Mat();
        detector.detect(image, detections);
        if (detections.empty()) {
            return detections;
        }
        if (detections.cols() != FACE_ROW_COLUMNS) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "Unexpected detector output format");
        }
        return detections;
    }

    private String extractFaceEmbedding(Mat image, Mat faceRow) {
        FaceRecognizerSF recognizer = FaceRecognizerSF.create(faceProperties.getRecognizerModelPath(), "");
        Mat alignedFace = new Mat();
        Mat feature = new Mat();
        try {
            recognizer.alignCrop(image, faceRow, alignedFace);
            recognizer.feature(alignedFace, feature);
            return encodeEmbedding(feature);
        } finally {
            alignedFace.release();
            feature.release();
        }
    }

    /**
     * Estimate quality from detector confidence, face area and blur.
     */
    private double computeQualityScore(Mat image, Mat faceRow) {
        double detectionScore = faceRow.get(0, FACE_SCORE_INDEX)[0];
        double areaRatio = extractFaceRect(faceRow, image).area() / Math.max(1D, image.cols() * image.rows());
        double areaScore = Math.min(1D, areaRatio / MIN_AREA_RATIO);
        double blurScore = computeBlurScore(image);
        return roundScore((detectionScore * 0.45D) + (areaScore * 0.25D) + (blurScore * 0.30D));
    }

    /**
     * Use brightness, contrast and face area to filter obvious spoof inputs.
     */
    private double computeLivenessScore(Mat image, Mat faceRow) {
        Rect faceRect = extractFaceRect(faceRow, image);
        Mat faceMat = new Mat(image, faceRect);
        Mat gray = new Mat();
        try {
            Imgproc.cvtColor(faceMat, gray, Imgproc.COLOR_BGR2GRAY);
            Scalar mean = Core.mean(gray);
            MatStats stats = calculateStdDev(gray);
            double brightness = normalizeRange(mean.val[0], 70D, 190D);
            double contrast = normalizeRange(stats.stdDev, 18D, 72D);
            double areaRatio = faceRect.area() / Math.max(1D, image.cols() * image.rows());
            double areaScore = Math.min(1D, areaRatio / MIN_AREA_RATIO);
            return roundScore((brightness * 0.35D) + (contrast * 0.35D) + (areaScore * 0.30D));
        } finally {
            faceMat.release();
            gray.release();
        }
    }

    private double computeBlurScore(Mat image) {
        Mat gray = new Mat();
        Mat laplacian = new Mat();
        try {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);
            MatStats stats = calculateStdDev(laplacian);
            double variance = stats.stdDev * stats.stdDev;
            return normalizeRange(variance, 120D, 900D);
        } finally {
            gray.release();
            laplacian.release();
        }
    }

    private MatStats calculateStdDev(Mat mat) {
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(mat, mean, stddev);
        double[] stdArray = stddev.get(0, 0);
        double value = stdArray == null || stdArray.length == 0 ? 0D : stdArray[0];
        mean.release();
        stddev.release();
        return new MatStats(value);
    }

    private Rect extractFaceRect(Mat faceRow, Mat image) {
        int x = (int) Math.max(0, Math.round(faceRow.get(0, FACE_BOX_X_INDEX)[0]));
        int y = (int) Math.max(0, Math.round(faceRow.get(0, FACE_BOX_Y_INDEX)[0]));
        int width = (int) Math.round(faceRow.get(0, FACE_BOX_WIDTH_INDEX)[0]);
        int height = (int) Math.round(faceRow.get(0, FACE_BOX_HEIGHT_INDEX)[0]);
        width = Math.max(1, Math.min(width, image.cols() - x));
        height = Math.max(1, Math.min(height, image.rows() - y));
        return new Rect(x, y, width, height);
    }

    private String encodeEmbedding(Mat feature) {
        int size = (int) (feature.total() * feature.channels());
        float[] vector = new float[size];
        feature.get(0, 0, vector);
        return Base64.getEncoder().encodeToString(floatArrayToBytes(vector));
    }

    private float[] decodeEmbedding(String embedding) {
        if (!StringUtils.hasText(embedding)) {
            return new float[0];
        }
        byte[] bytes = Base64.getDecoder().decode(embedding.trim());
        return bytesToFloatArray(bytes);
    }

    private byte[] floatArrayToBytes(float[] values) {
        byte[] bytes = new byte[values.length * Float.BYTES];
        for (int i = 0; i < values.length; i++) {
            int bits = Float.floatToIntBits(values[i]);
            int offset = i * Float.BYTES;
            bytes[offset] = (byte) bits;
            bytes[offset + 1] = (byte) (bits >>> 8);
            bytes[offset + 2] = (byte) (bits >>> 16);
            bytes[offset + 3] = (byte) (bits >>> 24);
        }
        return bytes;
    }

    private float[] bytesToFloatArray(byte[] bytes) {
        float[] values = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < values.length; i++) {
            int offset = i * Float.BYTES;
            int bits = (bytes[offset] & 0xFF)
                    | ((bytes[offset + 1] & 0xFF) << 8)
                    | ((bytes[offset + 2] & 0xFF) << 16)
                    | ((bytes[offset + 3] & 0xFF) << 24);
            values[i] = Float.intBitsToFloat(bits);
        }
        return values;
    }

    private double cosineSimilarity(float[] left, float[] right) {
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm <= 0D || rightNorm <= 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private BufferedImage decodeImage(String imageBase64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64.trim());
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "Unsupported image format");
            }
            return image;
        } catch (IllegalArgumentException | IOException ex) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "Cannot decode image content");
        }
    }

    private void ensureReady() {
        OpenCV.loadLocally();
        validateModelFile(faceProperties.getDetectorModelPath(), "Face detector model");
        validateModelFile(faceProperties.getRecognizerModelPath(), "Face recognizer model");
    }

    private BufferedImage normalizeImageSize(BufferedImage image) {
        int originWidth = image.getWidth();
        int originHeight = image.getHeight();
        int maxWidth = faceProperties.getInputWidth();
        int maxHeight = faceProperties.getInputHeight();
        double scale = Math.min(1D, Math.min(maxWidth * 1D / originWidth, maxHeight * 1D / originHeight));
        if (scale >= 0.999D) {
            return convertToBgr(image);
        }
        int targetWidth = Math.max(1, (int) Math.round(originWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(originHeight * scale));
        Image scaled = image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();
        return output;
    }

    private BufferedImage convertToBgr(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            return image;
        }
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = output.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return output;
    }

    private Mat bufferedImageToMat(BufferedImage image) {
        byte[] bytes = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, bytes);
        return mat;
    }

    private void validateModelFile(String path, String modelName) {
        if (!StringUtils.hasText(path)) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, modelName + " path is not configured");
        }
        File file = new File(path.trim());
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, modelName + " file does not exist: " + path);
        }
    }

    private double normalizeRange(double value, double min, double max) {
        if (value <= min) {
            return 0D;
        }
        if (value >= max) {
            return 1D;
        }
        return (value - min) / (max - min);
    }

    private double roundScore(double score) {
        return Math.max(0D, Math.min(1D, Math.round(score * 10000D) / 10000D));
    }

    private static final class MatStats {

        private final double stdDev;

        private MatStats(double stdDev) {
            this.stdDev = stdDev;
        }
    }
}
