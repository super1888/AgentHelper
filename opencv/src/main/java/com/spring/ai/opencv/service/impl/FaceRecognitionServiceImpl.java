package com.spring.ai.opencv.service.impl;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.opencv.domain.request.FaceLoginVerifyRequest;
import com.spring.ai.opencv.domain.response.FaceLoginVerifyResponse;
import com.spring.ai.opencv.service.FaceRecognitionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 人脸识别服务实现。
 *
 * <p>当前为 demo 级实现，后续可替换为真实人脸 embedding 与活体能力。</p>
 */
@Service
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    @Override
    public FaceLoginVerifyResponse verifyFace(FaceLoginVerifyRequest request) {
        if (request == null || !StringUtils.hasText(request.getImageBase64())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "图片内容不能为空");
        }
        String fingerprint = sha256(normalizeBase64(request.getImageBase64()));
        return FaceLoginVerifyResponse.builder()
                .faceDetected(true)
                .faceCount(1)
                .qualityScore(0.95D)
                .livenessScore(0.96D)
                .faceFingerprint(fingerprint)
                .build();
    }

    @Override
    public boolean isSameFace(String sourceFingerprint, String targetFingerprint) {
        return StringUtils.hasText(sourceFingerprint)
                && sourceFingerprint.equals(targetFingerprint);
    }

    private String normalizeBase64(String base64) {
        return base64 == null ? "" : base64.trim();
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "人脸指纹生成失败");
        }
    }
}
