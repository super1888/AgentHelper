package com.spring.ai.graph.service;


import com.spring.ai.graph.domian.request.ApprovalDecisionRequest;
import com.spring.ai.graph.domian.request.ApprovalWorkflowStartRequest;
import com.spring.ai.graph.domian.response.ApprovalWorkflowResponse;

/**
 * 审批工作流服务接口。
 */
public interface ApprovalWorkflowService {

    ApprovalWorkflowResponse startWorkflow(ApprovalWorkflowStartRequest request);

    ApprovalWorkflowResponse approve(String threadId, ApprovalDecisionRequest request) throws Exception;

    ApprovalWorkflowResponse queryWorkflow(String threadId);
}
