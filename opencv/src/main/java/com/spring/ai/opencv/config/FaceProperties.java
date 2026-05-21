package com.spring.ai.opencv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Face recognition configuration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent-helper.opencv.face")
public class FaceProperties {

    private String detectorModelPath;
    private String recognizerModelPath;
    private Float scoreThreshold = 0.9F;
    private Float nmsThreshold = 0.3F;
    private Integer topK = 5000;
    private Integer inputWidth = 640;
    private Integer inputHeight = 640;
    private Double qualityThreshold = 0.5D;
    private Boolean enableLivenessCheck = false;
    private Double livenessThreshold = 0.5D;
    private Double matchThreshold = 0.36D;
}
