package com.spring.ai.flowable.service;

import com.spring.ai.flowable.domain.request.FlowableApprovalDecisionRequest;
import com.spring.ai.flowable.domain.request.FlowableApprovalStartRequest;
import com.spring.ai.flowable.domain.response.FlowableApprovalDetailResponse;
import com.spring.ai.flowable.domain.response.FlowableApprovalStartResponse;
import com.spring.ai.flowable.domain.response.FlowableApprovalTaskResponse;
import java.util.List;

/**
 * Flowable 审批流程服务。
 */
public interface FlowableApprovalService {

    /**
     * 发起审批流程。
     */
    FlowableApprovalStartResponse startWorkflow(FlowableApprovalStartRequest request);

    /**
     * 处理审批动作。
     */
    FlowableApprovalDetailResponse decide(String taskId, FlowableApprovalDecisionRequest request);

    /**
     * 查询待办任务。
     */
    List<FlowableApprovalTaskResponse> listTodoTasks(String assignee);

    /**
     * 查询流程详情。
     */
    FlowableApprovalDetailResponse getWorkflowDetail(String processInstanceId);
}
