package com.spring.ai.skills.domain.dto;

import lombok.Data;

@Data
public class SkillObservabilityConfigDTO {

    private Integer debugEnabled;

    private String debugScript;

    private Integer realtimeLogStreaming;

    private String testCaseSummary;

    private Integer logEnabled;

    private Integer satisfactionEnabled;

    private String metricsPolicy;
}
