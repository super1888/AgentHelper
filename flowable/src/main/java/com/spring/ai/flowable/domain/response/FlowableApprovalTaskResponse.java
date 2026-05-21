package com.spring.ai.flowable.domain.response;

import java.util.Date;
import lombok.Data;

/**
 * 待办任务响应。
 */
@Data
public class FlowableApprovalTaskResponse {

    private String taskId;
    private String taskName;
    private String assignee;
    private String processInstanceId;
    private String processDefinitionId;
    private String businessKey;
    private Date createTime;
}
