package com.spring.ai.skills.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SkillWorkflowConfigDTO {

    private List<String> workflowSteps;

    private List<String> channelAdapters;

    private String orchestrationStrategy;
}
