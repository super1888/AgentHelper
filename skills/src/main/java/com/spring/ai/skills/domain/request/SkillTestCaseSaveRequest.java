package com.spring.ai.skills.domain.request;

import java.util.Map;
import lombok.Data;

@Data
public class SkillTestCaseSaveRequest {

    private Long skillId;

    private String caseName;

    private String inputText;

    private Map<String, Object> slotPayload;

    private String expectedIntent;

    private Integer expectedSuccess;

    private String expectedResponseContains;

    private String channelCode;

    private String locale;

    private Integer enabled;
}
