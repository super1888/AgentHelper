package com.spring.ai.flowable.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.flowable.domain.request.FlowableApprovalDecisionRequest;
import com.spring.ai.flowable.domain.request.FlowableApprovalStartRequest;
import com.spring.ai.flowable.domain.response.FlowableApprovalDetailResponse;
import com.spring.ai.flowable.domain.response.FlowableApprovalStartResponse;
import com.spring.ai.flowable.domain.response.FlowableApprovalTaskResponse;
import com.spring.ai.flowable.service.FlowableApprovalService;
import jakarta.annotation.Resource;

import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Flowable 审批工作流控制器。
 */
@RestController
@RequestMapping("/flowable/approval")
public class FlowableApprovalController {

    @Resource
    private FlowableApprovalService flowableApprovalService;

    /**
     * 发起审批流程。
     */
    @PostMapping("/start")
    public ApiResponse<FlowableApprovalStartResponse> start(@Valid @RequestBody FlowableApprovalStartRequest request) {
        return ApiResponse.success("审批流程已启动", flowableApprovalService.startWorkflow(request));
    }

    /**
     * 审批当前任务。
     */
    @PostMapping("/{taskId}/decision")
    public ApiResponse<FlowableApprovalDetailResponse> decision(
            @PathVariable String taskId,
            @Valid @RequestBody FlowableApprovalDecisionRequest request) {
        return ApiResponse.success("审批结果已提交", flowableApprovalService.decide(taskId, request));
    }

    /**
     * 查询待办任务。
     */
    @GetMapping("/tasks")
    public ApiResponse<List<FlowableApprovalTaskResponse>> tasks(@RequestParam(required = false) String assignee) {
        return ApiResponse.success(flowableApprovalService.listTodoTasks(assignee));
    }

    /**
     * 查询流程详情。
     */
    @GetMapping("/{processInstanceId}")
    public ApiResponse<FlowableApprovalDetailResponse> detail(@PathVariable String processInstanceId) {
        return ApiResponse.success(flowableApprovalService.getWorkflowDetail(processInstanceId));
    }
}
