package com.spring.ai.a2a.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class A2aRouteResponse {
    Long id;
    String routeCode;
    String routeName;
    String sourceAgentCode;
    String targetAgentCode;
    String taskType;
    String routeStatus;
    Integer priorityNo;
    Integer failoverEnabled;
    String fallbackAgentCodes;
    String remark;
    Long createTime;
}
