package com.spring.ai.interceptors.domain.request;

import java.util.Map;
import lombok.Data;

@Data
public class InterceptorDebugRequest {

    private Long interceptorId;

    private String requestPayloadJson;

    private Map<String, Object> contextPayload;

    private String sourceType;

    private String agentCode;

    private String sessionCode;
}
