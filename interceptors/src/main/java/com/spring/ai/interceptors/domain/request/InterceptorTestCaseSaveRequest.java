package com.spring.ai.interceptors.domain.request;

import java.util.Map;
import lombok.Data;

@Data
public class InterceptorTestCaseSaveRequest {

    private String caseName;

    private Map<String, Object> inputPayload;

    private Map<String, Object> contextPayload;

    private Integer expectedSuccess;

    private String expectedResponseContains;

    private Integer enabled;
}
