package com.spring.ai.opencv.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * YOLO 检测结果。
 */
@Data
@Builder
public class YoloDetectResponse {

    private Integer imageWidth;
    private Integer imageHeight;
    private Integer detectCount;
    private String modelName;
    private String modelVersion;
    private Long costTimeMs;
    private List<DetectionBoxResponse> detections;
}
