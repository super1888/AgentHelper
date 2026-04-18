package com.spring.ai.skills.domain.request;

import java.util.Map;
import lombok.Data;

@Data
public class SkillDebugRequest {

    private Long skillId;

    private String inputText;

    private String forcedIntent;

    private Map<String, Object> slotPayload;

    private Map<String, Object> contextPayload;

    private String channelCode;

    private String locale;
}
