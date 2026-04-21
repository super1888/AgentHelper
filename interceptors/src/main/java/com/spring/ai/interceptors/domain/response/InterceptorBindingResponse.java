package com.spring.ai.interceptors.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InterceptorBindingResponse {

    Long id;

    Long interceptorId;

    String interceptorCode;

    String bindingName;

    String bindingScope;

    String targetAgentCode;

    String targetModelCode;

    String environmentCode;

    Integer priorityNo;

    Integer enabled;

    String remark;

    Long createTime;
}
