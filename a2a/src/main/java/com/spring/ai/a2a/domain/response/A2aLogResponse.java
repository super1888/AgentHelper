package com.spring.ai.a2a.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class A2aLogResponse {
    Long id;
    String taskCode;
    String traceId;
    String sourceAgentCode;
    String targetAgentCode;
    String routeCode;
    String eventType;
    String executeStatus;
    Integer attemptNo;
    Integer retryIndex;
    Integer successFlag;
    Long elapsedMs;
    String failureReason;
    Long createTime;
}
