package com.spring.ai.skills.domain.dto;

import java.util.Map;
import java.util.List;
import lombok.Data;

@Data
public class SkillWorkflowConfigDTO {

    private Integer workflowEnabled;

    private List<String> workflowSteps;

    private List<Map<String, Object>> branchRules;

    private Integer loopEnabled;

    private List<String> childSkillCodes;

    private List<String> channelAdapters;

    private String orchestrationStrategy;
}
