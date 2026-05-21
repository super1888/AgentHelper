package com.spring.ai.flowable.domain.response;

import lombok.Data;

/**
 * 审批流程启动结果。
 */
@Data
public class FlowableApprovalStartResponse {

    private String processInstanceId;
    private String processDefinitionId;
    private String businessKey;
    private String currentTaskId;
    private String currentTaskName;
    private String currentTaskAssignee;
    private String processStatus;
}
