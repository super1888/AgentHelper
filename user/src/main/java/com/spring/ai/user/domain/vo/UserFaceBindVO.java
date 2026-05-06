package com.spring.ai.user.domain.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 人脸绑定结果。
 */
@Data
public class UserFaceBindVO {

    private Boolean bound;
    private Double qualityScore;
    private Double livenessScore;
    private String faceTemplateCode;
    private LocalDateTime lastVerifiedTime;
}
