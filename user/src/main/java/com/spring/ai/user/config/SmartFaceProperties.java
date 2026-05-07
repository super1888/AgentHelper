package com.spring.ai.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SmartJavaAI 人脸识别配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.user.face")
public class SmartFaceProperties {

    /**
     * 是否启用 SmartJavaAI 人脸识别。
     */
    private Boolean enabled = true;

    /**
     * 人脸检测模型路径，未配置时由 SmartJavaAI 按默认逻辑加载。
     */
    private String detectModelPath;

    /**
     * 人脸特征提取模型路径，未配置时由 SmartJavaAI 按默认逻辑加载。
     */
    private String recModelPath;

    /**
     * 推理设备类型，支持 CPU/GPU。
     */
    private String device = "CPU";

    /**
     * RetinaFace 检测阈值。
     */
    private Double detectionThreshold = 0.85D;

    /**
     * RetinaFace NMS 阈值。
     */
    private Double nmsThreshold = 0.45D;

    /**
     * 绑定和登录时要求的最小人脸质量分。
     * 这里直接复用检测得分作为质量分。
     */
    private Double qualityThreshold = 0.85D;

    /**
     * FaceNet 相似度阈值。
     */
    private Double matchThreshold = 0.80D;

    /**
     * 特征提取时是否裁剪人脸。
     */
    private Boolean cropFace = true;

    /**
     * 特征提取时是否做人脸对齐。
     */
    private Boolean align = true;
}
