package com.spring.ai.interceptors.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterceptorTestCaseResponse {

    Long id;

    Long interceptorId;

    String interceptorCode;

    String caseName;

    String inputPayloadJson;

    String contextPayloadJson;

    Integer expectedSuccess;

    String expectedResponseContains;

    Integer enabled;

    String lastRunStatus;

    Long lastRunDurationMs;

    Long lastRunAt;

    String lastResultJson;
}
