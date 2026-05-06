package com.spring.ai.opencv.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.opencv.domain.request.ImageDetectRequest;
import com.spring.ai.opencv.domain.response.YoloDetectResponse;
import com.spring.ai.opencv.service.YoloDetectionService;
import jakarta.validation.Valid;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图像分析控制器。
 */
@RestController
@RequestMapping("/image")
public class ImageAnalysisController {

    @Resource
    private YoloDetectionService yoloDetectionService;

    @PostMapping("/detect")
    public ApiResponse<YoloDetectResponse> detect(@Valid @RequestBody ImageDetectRequest request) {
        return ApiResponse.success("识别成功", yoloDetectionService.detectKitchenIngredients(request));
    }
}
