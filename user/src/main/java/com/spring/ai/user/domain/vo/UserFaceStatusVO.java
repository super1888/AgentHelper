package com.spring.ai.user.domain.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 人脸状态结果。
 */
@Data
public class UserFaceStatusVO {

    private Boolean bound;
    private String faceTemplateCode;
    private String status;
    private LocalDateTime lastVerifiedTime;
}
