package com.spring.ai.user.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 人脸登录请求。
 */
@Data
public class UserFaceLoginRequest {

    @NotBlank(message = "图片内容不能为空")
    private String imageBase64;

    @NotBlank(message = "图片格式不能为空")
    private String imageFormat;

    private String deviceId;

    private String clientIp;

    private Boolean silentLogin;
}
