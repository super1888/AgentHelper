package com.spring.ai.skills.domain.dto;

import lombok.Data;

@Data
public class SkillParameterConfigDTO {

    private String parameterName;

    private String parameterType;

    private Integer required;

    private String defaultValue;

    private String description;
}
