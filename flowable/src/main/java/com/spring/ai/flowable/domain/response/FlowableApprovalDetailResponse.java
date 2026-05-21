package com.spring.ai.flowable.domain.response;

import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 流程详情响应。
 */
@Data
public class FlowableApprovalDetailResponse {

    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;
    private String businessKey;
    private String processStatus;
    private Date startTime;
    private Date endTime;
    private Map<String, Object> variables;
    private List<FlowableApprovalTaskResponse> currentTasks;
    private List<FlowableApprovalHistoryTaskResponse> historyTasks;
}
