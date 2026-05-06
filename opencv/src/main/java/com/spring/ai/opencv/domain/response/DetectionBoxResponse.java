package com.spring.ai.opencv.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 检测框响应。
 */
@Data
@Builder
public class DetectionBoxResponse {

    private String label;
    private String classCode;
    private Double confidence;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private Double areaRatio;
    private Integer estimatedCount;
    private String ingredientCategory;
}
