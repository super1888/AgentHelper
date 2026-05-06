package com.spring.ai.opencv.service.impl;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.opencv.domain.request.ImageDetectRequest;
import com.spring.ai.opencv.domain.response.DetectionBoxResponse;
import com.spring.ai.opencv.domain.response.YoloDetectResponse;
import com.spring.ai.opencv.integration.yolo.YoloOnnxInferenceClient;
import com.spring.ai.opencv.service.YoloDetectionService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * YOLO 检测服务实现。
 */
@Service
public class YoloDetectionServiceImpl implements YoloDetectionService {

    private final YoloOnnxInferenceClient yoloOnnxInferenceClient;

    public YoloDetectionServiceImpl(YoloOnnxInferenceClient yoloOnnxInferenceClient) {
        this.yoloOnnxInferenceClient = yoloOnnxInferenceClient;
    }

    @Override
    public YoloDetectResponse detectKitchenIngredients(ImageDetectRequest request) {
        if (request == null || !StringUtils.hasText(request.getImageBase64())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "图片内容不能为空");
        }
        long startTime = System.currentTimeMillis();
        List<DetectionBoxResponse> detections = yoloOnnxInferenceClient.infer(request);
        return YoloDetectResponse.builder()
                .imageWidth(1280)
                .imageHeight(720)
                .detectCount(detections.size())
                .modelName(yoloOnnxInferenceClient.resolveModelName())
                .modelVersion("demo-1.0")
                .costTimeMs(System.currentTimeMillis() - startTime)
                .detections(detections)
                .build();
    }
}
