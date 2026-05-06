package com.spring.ai.opencv.service.impl;

import com.spring.ai.opencv.domain.dto.YoloRawPrediction;
import com.spring.ai.opencv.domain.response.DetectionBoxResponse;
import com.spring.ai.opencv.config.YoloProperties;
import com.spring.ai.opencv.service.YoloPostProcessService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * YOLO 后处理服务实现。
 */
@Service
public class YoloPostProcessServiceImpl implements YoloPostProcessService {

    private static final String[] CLASS_CODES = {
            "EGG", "TOMATO", "POTATO", "ONION", "GARLIC", "SCALLION", "PORK", "PEPPER"
    };

    private static final String[] LABELS = {
            "egg", "tomato", "potato", "onion", "garlic", "scallion", "pork", "pepper"
    };

    private static final String[] CATEGORIES = {
            "PROTEIN", "VEGETABLE", "VEGETABLE", "VEGETABLE", "SEASONING", "SEASONING", "MEAT", "VEGETABLE"
    };

    private final YoloProperties yoloProperties;

    public YoloPostProcessServiceImpl(YoloProperties yoloProperties) {
        this.yoloProperties = yoloProperties;
    }

    @Override
    public List<DetectionBoxResponse> postProcess(List<YoloRawPrediction> predictions, int imageWidth, int imageHeight) {
        List<DetectionBoxResponse> results = new ArrayList<>();
        if (predictions == null || predictions.isEmpty()) {
            return results;
        }
        Map<Integer, YoloProperties.YoloClassMapping> mappingMap = buildMappingMap();
        for (YoloRawPrediction prediction : predictions) {
            int classIndex = prediction.getClassIndex() == null ? -1 : prediction.getClassIndex();
            if (classIndex < 0) {
                continue;
            }
            int width = Math.max(1, safeInt(prediction.getWidth()));
            int height = Math.max(1, safeInt(prediction.getHeight()));
            int x = Math.max(0, (int) Math.round(safeDouble(prediction.getCenterX()) - width / 2.0D));
            int y = Math.max(0, (int) Math.round(safeDouble(prediction.getCenterY()) - height / 2.0D));
            double areaRatio = imageWidth <= 0 || imageHeight <= 0
                    ? 0D
                    : (double) (width * height) / (double) (imageWidth * imageHeight);
            YoloClassDescriptor descriptor = resolveDescriptor(classIndex, mappingMap);
            if (descriptor == null) {
                continue;
            }
            results.add(DetectionBoxResponse.builder()
                    .label(descriptor.label)
                    .classCode(descriptor.classCode)
                    .confidence(prediction.getConfidence())
                    .x(x)
                    .y(y)
                    .width(width)
                    .height(height)
                    .areaRatio(areaRatio)
                    .estimatedCount(1)
                    .ingredientCategory(descriptor.ingredientCategory)
                    .build());
        }
        return results;
    }

    private int safeInt(Double value) {
        return value == null ? 1 : value.intValue();
    }

    private double safeDouble(Double value) {
        return value == null ? 0D : value;
    }

    private Map<Integer, YoloProperties.YoloClassMapping> buildMappingMap() {
        Map<Integer, YoloProperties.YoloClassMapping> mappingMap = new HashMap<>();
        if (yoloProperties.getClasses() != null) {
            for (YoloProperties.YoloClassMapping mapping : yoloProperties.getClasses()) {
                if (mapping != null && mapping.getClassIndex() != null) {
                    mappingMap.put(mapping.getClassIndex(), mapping);
                }
            }
        }
        return mappingMap;
    }

    private YoloClassDescriptor resolveDescriptor(int classIndex, Map<Integer, YoloProperties.YoloClassMapping> mappingMap) {
        YoloProperties.YoloClassMapping mapping = mappingMap.get(classIndex);
        if (mapping != null) {
            return new YoloClassDescriptor(
                    mapping.getLabel() == null ? defaultValue(LABELS, classIndex, "class-" + classIndex) : mapping.getLabel(),
                    mapping.getClassCode() == null ? defaultValue(CLASS_CODES, classIndex, "CLASS_" + classIndex) : mapping.getClassCode(),
                    mapping.getIngredientCategory() == null ? defaultValue(CATEGORIES, classIndex, "UNKNOWN") : mapping.getIngredientCategory()
            );
        }
        if (classIndex < LABELS.length && classIndex < CLASS_CODES.length && classIndex < CATEGORIES.length) {
            return new YoloClassDescriptor(LABELS[classIndex], CLASS_CODES[classIndex], CATEGORIES[classIndex]);
        }
        return null;
    }

    private String defaultValue(String[] values, int index, String fallback) {
        return index >= 0 && index < values.length ? values[index] : fallback;
    }

    private record YoloClassDescriptor(String label, String classCode, String ingredientCategory) {
    }
}
