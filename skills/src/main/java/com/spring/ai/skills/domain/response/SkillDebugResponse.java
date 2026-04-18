package com.spring.ai.skills.domain.response;

import com.spring.ai.skills.domain.dto.SkillDebugTraceStepDTO;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillDebugResponse {

    private Long skillId;

    private String skillCode;

    private String matchedIntent;

    private Double confidenceScore;

    private Integer successFlag;

    private String responseText;

    private String failureReason;

    private Long elapsedMs;

    private Map<String, Object> resolvedSlots;

    private Map<String, Object> contextPayload;

    private List<SkillDebugTraceStepDTO> traceSteps;
}
