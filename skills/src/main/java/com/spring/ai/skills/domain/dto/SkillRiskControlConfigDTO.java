package com.spring.ai.skills.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SkillRiskControlConfigDTO {

    private Integer sensitiveWordEnabled;

    private Integer parameterDesensitizeEnabled;

    private List<String> desensitizeFields;

    private Integer rateLimitPerMinute;

    private Integer highRiskConfirmation;

    private String secondaryConfirmationPrompt;

    private Integer auditEnabled;
}
