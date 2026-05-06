package com.spring.ai.opencv.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 人脸校验结果。
 */
@Data
@Builder
public class FaceLoginVerifyResponse {

    private Boolean faceDetected;
    private Integer faceCount;
    private Double qualityScore;
    private Double livenessScore;
    private String faceFingerprint;
}
