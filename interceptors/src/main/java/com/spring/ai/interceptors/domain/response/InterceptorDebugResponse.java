package com.spring.ai.interceptors.domain.response;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterceptorDebugResponse {

    Long interceptorId;

    String interceptorCode;

    String interceptorName;

    Integer successFlag;

    String executeStatus;

    String responseText;

    String failureReason;

    Long elapsedMs;

    String requestPayloadJson;

    String responsePayloadJson;

    Map<String, Object> tracePayload;
}
