package com.spring.ai.interceptors.domain.request;

import lombok.Data;

@Data
public class InterceptorLogQueryRequest {

    private Long interceptorId;

    private String sourceType;

    private Integer successFlag;
}
