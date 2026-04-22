package com.spring.ai.a2a.domain.request;

import lombok.Data;

@Data
public class A2aRouteSaveRequest {
    private String routeCode;
    private String routeName;
    private String sourceAgentCode;
    private String targetAgentCode;
    private String taskType;
    private String routeStatus;
    private Integer priorityNo;
    private Integer failoverEnabled;
    private String fallbackAgentCodes;
    private String remark;
}
