package com.spring.quickstart.workflow.approval.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.quickstart.workflow.approval.domain.request.ApprovalDecisionRequest;
import com.spring.quickstart.workflow.approval.domain.request.ApprovalWorkflowStartRequest;
import com.spring.quickstart.workflow.approval.domain.response.ApprovalWorkflowResponse;
import com.spring.quickstart.workflow.approval.service.ApprovalWorkflowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审批 Graph 示例控制器。
 *
 * <p>对外暴露三个核心接口：
 * 1. 发起审批
 * 2. 人工审批并恢复 Graph
 * 3. 查询当前工作流状态
 *
 * <p>你后续学习时，建议先用 /start 发起，再看返回的 threadId，
 * 然后调用 /{threadId} 观察中断点，再调用 /{threadId}/approve 继续执行。</p>
 */
@RestController
@RequestMapping("/workflow/approval")
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService approvalWorkflowService;

    public ApprovalWorkflowController(ApprovalWorkflowService approvalWorkflowService) {
        this.approvalWorkflowService = approvalWorkflowService;
    }

    /**
     * 发起审批。
     */
    @PostMapping("/start")
    public ApiResponse<ApprovalWorkflowResponse> start(@RequestBody ApprovalWorkflowStartRequest request) {
        return ApiResponse.success("审批工作流已启动", approvalWorkflowService.startWorkflow(request));
    }

    /**
     * 审批并恢复执行。
     */
    @PostMapping("/{threadId}/approve")
    public ApiResponse<ApprovalWorkflowResponse> approve(
            @PathVariable String threadId,
            @RequestBody ApprovalDecisionRequest request) throws Exception {
        return ApiResponse.success("审批动作已提交", approvalWorkflowService.approve(threadId, request));
    }

    /**
     * 查询当前审批工作流状态。
     */
    @GetMapping("/{threadId}")
    public ApiResponse<ApprovalWorkflowResponse> query(@PathVariable String threadId) {
        return ApiResponse.success(approvalWorkflowService.queryWorkflow(threadId));
    }
}
