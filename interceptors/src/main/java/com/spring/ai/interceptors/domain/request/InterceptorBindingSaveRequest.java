package com.spring.ai.interceptors.domain.request;

import lombok.Data;

@Data
public class InterceptorBindingSaveRequest {

    private String bindingName;

    private String bindingScope;

    private String targetAgentCode;

    private String targetModelCode;

    private String environmentCode;

    private Integer priorityNo;

    private Integer enabled;

    private String remark;
}
