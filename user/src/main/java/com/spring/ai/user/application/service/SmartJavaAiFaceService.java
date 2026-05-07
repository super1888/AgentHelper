package com.spring.ai.user.application.service;

import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.entity.DetectionResponse;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.enums.DeviceEnum;
import cn.smartjavaai.face.config.FaceDetConfig;
import cn.smartjavaai.face.config.FaceRecConfig;
import cn.smartjavaai.face.enums.FaceDetModelEnum;
import cn.smartjavaai.face.enums.FaceRecModelEnum;
import cn.smartjavaai.face.factory.FaceDetModelFactory;
import cn.smartjavaai.face.factory.FaceRecModelFactory;
import cn.smartjavaai.face.model.facedect.FaceDetModel;
import cn.smartjavaai.face.model.facerec.FaceRecModel;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.opencv.domain.request.FaceLoginVerifyRequest;
import com.spring.ai.opencv.domain.response.FaceLoginVerifyResponse;
import com.spring.ai.user.config.SmartFaceProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 基于 SmartJavaAI 的人脸识别服务。
 */
@Service
@ConditionalOnClass(name = {
        "cn.smartjavaai.common.enums.DeviceEnum",
        "cn.smartjavaai.face.factory.FaceDetModelFactory",
        "cn.smartjavaai.face.factory.FaceRecModelFactory"
})
public class SmartJavaAiFaceService implements UserFaceRecognitionService {

    private static final String EMBEDDING_VERSION = "smartjavaai-retinaface-facenet-1.0.19";

    @Resource
    private SmartFaceProperties smartFaceProperties;

    private final Object modelMonitor = new Object();

    private FaceDetModel faceDetModel;
    private FaceRecModel faceRecModel;

    /**
     * 初始化人脸检测与特征提取模型。
     */
    @PostConstruct
    public void init() {
        if (!Boolean.TRUE.equals(smartFaceProperties.getEnabled())) {
            return;
        }
        try {
            DeviceEnum deviceEnum = resolveDevice();

            FaceDetConfig faceDetConfig = new FaceDetConfig(FaceDetModelEnum.RETINA_FACE);
            faceDetConfig.setConfidenceThreshold(smartFaceProperties.getDetectionThreshold());
            faceDetConfig.setNmsThresh(smartFaceProperties.getNmsThreshold());
            faceDetConfig.setDevice(deviceEnum);
            if (StringUtils.hasText(smartFaceProperties.getDetectModelPath())) {
                faceDetConfig.setModelPath(smartFaceProperties.getDetectModelPath().trim());
            }
            faceDetModel = FaceDetModelFactory.getInstance().getModel(faceDetConfig);

            FaceRecConfig faceRecConfig = new FaceRecConfig(FaceRecModelEnum.FACENET_MODEL);
            faceRecConfig.setDevice(deviceEnum);
            faceRecConfig.setCropFace(Boolean.TRUE.equals(smartFaceProperties.getCropFace()));
            faceRecConfig.setAlign(Boolean.TRUE.equals(smartFaceProperties.getAlign()));
            faceRecConfig.setAutoLoadFace(false);
            faceRecConfig.setDetectModel(faceDetModel);
            if (StringUtils.hasText(smartFaceProperties.getRecModelPath())) {
                faceRecConfig.setModelPath(smartFaceProperties.getRecModelPath().trim());
            }
            faceRecModel = FaceRecModelFactory.getInstance().getModel(faceRecConfig);
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "SmartJavaAI 人脸模型初始化失败：" + exception.getMessage(),
                    exception
            );
        }
    }

    /**
     * 释放模型资源。
     */
    @PreDestroy
    public void destroy() {
        closeQuietly(faceRecModel);
        closeQuietly(faceDetModel);
    }

    /**
     * 校验人脸图像并提取特征向量。
     */
    public FaceLoginVerifyResponse verifyFace(FaceLoginVerifyRequest request) {
        if (!Boolean.TRUE.equals(smartFaceProperties.getEnabled())) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "SmartJavaAI 人脸识别已禁用");
        }
        if (request == null || !StringUtils.hasText(request.getImageBase64())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "图片内容不能为空");
        }
        ensureModelReady();
        BufferedImage image = decodeImage(request.getImageBase64());
        synchronized (modelMonitor) {
            R<DetectionResponse> detectResult = faceDetModel.detect(image);
            DetectionResponse detectionResponse = unwrapRequiredResult(detectResult, "人脸检测失败");
            List<DetectionInfo> detectionInfoList = detectionResponse.getDetectionInfoList();
            if (detectionInfoList == null) {
                detectionInfoList = Collections.emptyList();
            }
            int faceCount = detectionInfoList.size();
            if (faceCount <= 0) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "未检测到人脸");
            }
            if (faceCount > 1) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请保持图片中仅有一张人脸");
            }

            DetectionInfo topFace = detectionInfoList.get(0);
            double qualityScore = topFace == null ? 0D : topFace.getScore();
            if (qualityScore < smartFaceProperties.getQualityThreshold()) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "人脸图片质量过低");
            }

            R<float[]> featureResult = faceRecModel.extractTopFaceFeature(image);
            float[] feature = unwrapRequiredResult(featureResult, "人脸特征提取失败");
            if (feature.length == 0) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "人脸特征提取失败");
            }
            return FaceLoginVerifyResponse.builder()
                    .faceDetected(true)
                    .faceCount(faceCount)
                    .qualityScore(roundScore(qualityScore))
                    .livenessScore(null)
                    .faceEmbedding(encodeEmbedding(feature))
                    .build();
        }
    }

    /**
     * 判断两个人脸特征是否匹配。
     */
    public boolean isSameFace(String sourceEmbedding, String targetEmbedding) {
        ensureModelReady();
        float[] sourceVector = decodeEmbedding(sourceEmbedding);
        float[] targetVector = decodeEmbedding(targetEmbedding);
        if (sourceVector.length == 0 || sourceVector.length != targetVector.length) {
            return false;
        }
        synchronized (modelMonitor) {
            return faceRecModel.calculSimilar(sourceVector, targetVector) >= smartFaceProperties.getMatchThreshold();
        }
    }

    /**
     * 获取特征维度。
     */
    public int resolveEmbeddingDimension(String embedding) {
        return decodeEmbedding(embedding).length;
    }

    /**
     * 获取当前特征版本。
     */
    public String resolveEmbeddingVersion() {
        return EMBEDDING_VERSION;
    }

    private void ensureModelReady() {
        if (!Boolean.TRUE.equals(smartFaceProperties.getEnabled())) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "SmartJavaAI 人脸识别已禁用");
        }
        if (faceDetModel == null || faceRecModel == null) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "SmartJavaAI 人脸模型未初始化");
        }
    }

    private DeviceEnum resolveDevice() {
        String device = smartFaceProperties.getDevice();
        if (!StringUtils.hasText(device)) {
            return DeviceEnum.CPU;
        }
        try {
            return DeviceEnum.valueOf(device.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "不支持的人脸推理设备类型：" + device);
        }
    }

    private BufferedImage decodeImage(String imageBase64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(stripDataPrefix(imageBase64.trim()));
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "不支持的图片格式");
            }
            return image;
        } catch (IllegalArgumentException | IOException exception) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "图片内容解码失败");
        }
    }

    private String stripDataPrefix(String imageBase64) {
        int commaIndex = imageBase64.indexOf(',');
        if (commaIndex > 0 && imageBase64.startsWith("data:")) {
            return imageBase64.substring(commaIndex + 1);
        }
        return imageBase64;
    }

    private <T> T unwrapResult(R<T> result, String message) {
        if (result == null || !result.isSuccess()) {
            String errorMessage = result == null ? message : message + "：" + result.getMessage();
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, errorMessage);
        }
        return result.getData();
    }

    private <T> T unwrapRequiredResult(R<T> result, String message) {
        T data = unwrapResult(result, message);
        if (data == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, message + "：模型返回空结果");
        }
        return data;
    }

    private String encodeEmbedding(float[] feature) {
        byte[] bytes = new byte[feature.length * Float.BYTES];
        for (int index = 0; index < feature.length; index++) {
            int bits = Float.floatToIntBits(feature[index]);
            int offset = index * Float.BYTES;
            bytes[offset] = (byte) bits;
            bytes[offset + 1] = (byte) (bits >>> 8);
            bytes[offset + 2] = (byte) (bits >>> 16);
            bytes[offset + 3] = (byte) (bits >>> 24);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    private float[] decodeEmbedding(String embedding) {
        if (!StringUtils.hasText(embedding)) {
            return new float[0];
        }
        byte[] bytes = Base64.getDecoder().decode(embedding.trim());
        float[] values = new float[bytes.length / Float.BYTES];
        for (int index = 0; index < values.length; index++) {
            int offset = index * Float.BYTES;
            int bits = (bytes[offset] & 0xFF)
                    | ((bytes[offset + 1] & 0xFF) << 8)
                    | ((bytes[offset + 2] & 0xFF) << 16)
                    | ((bytes[offset + 3] & 0xFF) << 24);
            values[index] = Float.intBitsToFloat(bits);
        }
        return values;
    }

    private double roundScore(double score) {
        return Math.max(0D, Math.min(1D, Math.round(score * 10000D) / 10000D));
    }

    private void closeQuietly(AutoCloseable target) {
        if (target == null) {
            return;
        }
        try {
            target.close();
        } catch (Exception ignored) {
            // 关闭阶段忽略资源释放异常，避免影响容器退出流程。
        }
    }
}
