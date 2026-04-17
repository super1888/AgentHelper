package com.spring.ai.skills.domain.dto;

import lombok.Data;

@Data
public class SkillExecutionConfigDTO {

    private String executionType;

    private String apiEndpoint;

    private String httpMethod;

    private String functionName;

    private String timeoutMs;

    private String requestTemplate;

    private String responseMapping;
}
