package com.spring.ai.skills.domain.dto;

import lombok.Data;

@Data
public class SkillObservabilityConfigDTO {

    private Integer debugEnabled;

    private String debugScript;

    private String testCaseSummary;

    private Integer logEnabled;

    private String metricsPolicy;
}
