package com.spring.ai.skills.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SkillParameterConfigDTO {

    private String parameterName;

    private String parameterType;

    private Integer required;

    private String defaultValue;

    private String description;

    private List<String> enumOptions;

    private String validationRegex;

    private Integer minLength;

    private Integer maxLength;

    private Double minValue;

    private Double maxValue;

    private String promptWhenMissing;

    private Integer sensitiveFlag;

    private String desensitizeRule;
}
