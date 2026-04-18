package com.spring.ai.skills.domain.dto;

import java.util.Map;
import lombok.Data;

@Data
public class SkillChannelAdaptationDTO {

    private String channelCode;

    private String locale;

    private String successTemplate;

    private String failureTemplate;

    private String voiceTemplate;

    private Map<String, Object> styleConfig;
}
