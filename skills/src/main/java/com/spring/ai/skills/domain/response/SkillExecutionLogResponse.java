package com.spring.ai.skills.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillExecutionLogResponse {

    private Long id;

    private Long skillId;

    private String skillCode;

    private String skillName;

    private String sourceType;

    private Long sourceId;

    private String traceId;

    private String sessionCode;

    private String channelCode;

    private String locale;

    private String inputText;

    private String matchedIntent;

    private Double confidenceScore;

    private String requestPayloadJson;

    private String responsePayloadJson;

    private String tracePayloadJson;

    private String executeStatus;

    private Integer successFlag;

    private Long elapsedMs;

    private String failureReason;

    private Integer satisfactionLevel;

    private String operatorUserName;

    private Long createTime;
}
