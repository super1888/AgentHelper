package com.spring.ai.a2a.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class A2aTaskResponse {
    Long id;
    String taskCode;
    String taskType;
    String sourceAgentCode;
    String targetAgentCode;
    String routeCode;
    String taskStatus;
    String requestPayloadJson;
    String responsePayloadJson;
    String failureReason;
    Long elapsedMs;
    Long createTime;
}
