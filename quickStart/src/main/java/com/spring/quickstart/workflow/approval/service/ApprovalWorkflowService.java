package com.spring.quickstart.workflow.approval.service;

import com.spring.quickstart.workflow.approval.domain.request.ApprovalDecisionRequest;
import com.spring.quickstart.workflow.approval.domain.request.ApprovalWorkflowStartRequest;
import com.spring.quickstart.workflow.approval.domain.response.ApprovalWorkflowResponse;

/**
 * 审批工作流服务接口。
 */
public interface ApprovalWorkflowService {

    ApprovalWorkflowResponse startWorkflow(ApprovalWorkflowStartRequest request);

    ApprovalWorkflowResponse approve(String threadId, ApprovalDecisionRequest request) throws Exception;

    ApprovalWorkflowResponse queryWorkflow(String threadId);
}
