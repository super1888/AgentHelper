package com.spring.ai.opencv.service;

import com.spring.ai.opencv.domain.request.ImageDetectRequest;
import com.spring.ai.opencv.domain.response.YoloDetectResponse;

/**
 * YOLO 检测服务。
 */
public interface YoloDetectionService {

    YoloDetectResponse detectKitchenIngredients(ImageDetectRequest request);
}
