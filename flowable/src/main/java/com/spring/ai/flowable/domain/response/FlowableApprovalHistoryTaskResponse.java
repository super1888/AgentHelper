package com.spring.ai.flowable.domain.response;

import java.util.Date;
import lombok.Data;

/**
 * 流程历史任务响应。
 */
@Data
public class FlowableApprovalHistoryTaskResponse {

    private String taskId;
    private String taskName;
    private String assignee;
    private Date startTime;
    private Date endTime;
    private String deleteReason;
}
