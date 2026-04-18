package com.spring.ai.skills.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillDebugTraceStepDTO {

    private String stepName;

    private String stepStatus;

    private String detail;
}
