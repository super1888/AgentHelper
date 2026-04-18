package com.spring.ai.skills.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillTestCaseResponse {

    private Long id;

    private Long skillId;

    private String skillCode;

    private String caseName;

    private String inputText;

    private String slotPayloadJson;

    private String expectedIntent;

    private Integer expectedSuccess;

    private String expectedResponseContains;

    private String channelCode;

    private String locale;

    private Integer enabled;

    private String lastRunStatus;

    private Long lastRunDurationMs;

    private Long lastRunAt;

    private String lastResultJson;
}
