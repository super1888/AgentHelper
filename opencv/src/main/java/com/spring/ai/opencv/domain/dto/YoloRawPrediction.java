package com.spring.ai.opencv.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * YOLO 原始预测对象。
 */
@Data
@Builder
public class YoloRawPrediction {

    private Integer classIndex;
    private Double confidence;
    private Double centerX;
    private Double centerY;
    private Double width;
    private Double height;
}
