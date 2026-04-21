package com.spring.ai.interceptors.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterceptorExecutionLogResponse {

    Long id;

    Long interceptorId;

    String interceptorCode;

    String interceptorName;

    String sourceType;

    Long sourceId;

    String traceId;

    String agentCode;

    String sessionCode;

    String requestPayloadJson;

    String contextPayloadJson;

    String responsePayloadJson;

    String executeStatus;

    Integer successFlag;

    Long elapsedMs;

    String failureReason;

    String operatorUserName;

    Long createTime;
}
