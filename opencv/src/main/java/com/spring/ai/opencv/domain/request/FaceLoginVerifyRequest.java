package com.spring.ai.opencv.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 人脸图像校验请求。
 */
@Data
public class FaceLoginVerifyRequest {

    @NotBlank(message = "图片内容不能为空")
    private String imageBase64;

    @NotBlank(message = "图片格式不能为空")
    private String imageFormat;

    private String deviceId;

    private String clientIp;
}
