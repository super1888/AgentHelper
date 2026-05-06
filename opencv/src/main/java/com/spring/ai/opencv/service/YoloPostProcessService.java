package com.spring.ai.opencv.service;

import com.spring.ai.opencv.domain.dto.YoloRawPrediction;
import com.spring.ai.opencv.domain.response.DetectionBoxResponse;
import java.util.List;

/**
 * YOLO 后处理服务。
 */
public interface YoloPostProcessService {

    List<DetectionBoxResponse> postProcess(
            List<YoloRawPrediction> predictions,
            int imageWidth,
            int imageHeight
    );
}
