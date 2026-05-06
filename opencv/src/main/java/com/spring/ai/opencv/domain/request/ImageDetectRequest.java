package com.spring.ai.opencv.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 图像检测请求。
 */
@Data
public class ImageDetectRequest {

    @NotBlank(message = "图片内容不能为空")
    private String imageBase64;

    @NotBlank(message = "图片格式不能为空")
    private String imageFormat;

    private String businessScene = "KITCHEN_ASSISTANT";
}
