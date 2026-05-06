package com.spring.ai.opencv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * YOLO 检测配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent-helper.opencv.yolo")
public class YoloProperties {

    private String runtime = "onnxruntime";
    private String modelPath;
    private String modelName = "yolo-kitchen-v1";
    private Double confidenceThreshold = 0.35D;
    private Double iouThreshold = 0.45D;
    private Integer maxDetections = 100;
    private Integer inputWidth = 640;
    private Integer inputHeight = 640;
    private List<YoloClassMapping> classes = new ArrayList<>();

    @Data
    public static class YoloClassMapping {

        private Integer classIndex;
        private String label;
        private String classCode;
        private String ingredientCategory;
    }
}
