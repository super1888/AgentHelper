package com.spring.ai.skills.domain.dto;

import java.util.Map;
import lombok.Data;

@Data
public class SkillReplyTemplateDTO {

    private String templateCode;

    private String templateName;

    private String templateType;

    private String channelCode;

    private String locale;

    private String templateContent;

    private Map<String, Object> richPayload;
}
