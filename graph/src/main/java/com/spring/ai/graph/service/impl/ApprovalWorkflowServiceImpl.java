package com.spring.ai.graph.service.impl;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.spring.ai.common.enums.graph.ApprovalWorkflowCheckpointModeEnum;

import com.spring.ai.graph.config.ThreadIdSupport;
import com.spring.ai.graph.domian.request.ApprovalDecisionRequest;
import com.spring.ai.graph.domian.request.ApprovalWorkflowStartRequest;
import com.spring.ai.graph.domian.response.ApprovalWorkflowResponse;
import com.spring.ai.graph.service.ApprovalWorkflowService;
import com.spring.ai.graph.stateKeys.ApprovalWorkflowStateKeys;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审批工作流服务实现。
 *
 * <p>Controller 只负责收发请求，真正的 Graph 调用、状态查询、审批恢复都集中在这里，
 * 方便你以后把它替换成正式业务模块。</p>
 */
@Slf4j
@Service
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {

    @Resource(name = "approvalWorkflowGraph")
    private CompiledGraph approvalWorkflowGraph;

    @Override
    public ApprovalWorkflowResponse startWorkflow(ApprovalWorkflowStartRequest request) {
        ApprovalWorkflowCheckpointModeEnum checkpointMode = ThreadIdSupport
                .normalizeMode(request.getCheckpointType());
        String threadId = ThreadIdSupport.createThreadId(checkpointMode);
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        Map<String, Object> input = new LinkedHashMap<>();
        input.put(ApprovalWorkflowStateKeys.REQUEST_NO, request.getRequestNo());
        input.put(ApprovalWorkflowStateKeys.APPLICANT_NAME, request.getApplicantName());
        input.put(ApprovalWorkflowStateKeys.APPLICANT_EMAIL, request.getApplicantEmail());
        input.put(ApprovalWorkflowStateKeys.DEPARTMENT, request.getDepartment());
        input.put(ApprovalWorkflowStateKeys.REASON, request.getReason());
        input.put(ApprovalWorkflowStateKeys.LEAVE_DAYS, request.getLeaveDays());
        input.put(ApprovalWorkflowStateKeys.AMOUNT, request.getAmount());
        input.put(ApprovalWorkflowStateKeys.APPROVER_NAME, request.getApproverName());
        input.put(ApprovalWorkflowStateKeys.APPROVER_EMAIL, request.getApproverEmail());

        approvalWorkflowGraph.stream(input, config)
                .doOnNext(nodeOutput -> log.info("审批工作流节点执行, threadId={}, node={}", threadId, nodeOutput.node()))
                .blockLast();

        return buildResponse(threadId);
    }

    @Override
    public ApprovalWorkflowResponse approve(String threadId, ApprovalDecisionRequest request) throws Exception {
        if (request.getApproved() == null) {
            throw new IllegalArgumentException("审批结果不能为空");
        }

        RunnableConfig invokeConfig = buildThreadConfig(threadId);
        Map<String, Object> stateUpdates = new LinkedHashMap<>();
        stateUpdates.put(ApprovalWorkflowStateKeys.APPROVAL_DECISION, request.getApproved());
        stateUpdates.put(ApprovalWorkflowStateKeys.APPROVAL_COMMENT,
                request.getComment() == null ? "" : request.getComment());

        RunnableConfig updatedConfig = approvalWorkflowGraph.updateState(
                invokeConfig,
                stateUpdates,
                null);

        approvalWorkflowGraph.stream(Map.of(), updatedConfig)
                .doOnNext(nodeOutput -> log.info("审批恢复执行, threadId={}, node={}", threadId, nodeOutput.node()))
                .blockLast();

        return buildResponse(threadId);
    }

    @Override
    public ApprovalWorkflowResponse queryWorkflow(String threadId) {
        return buildResponse(threadId);
    }

    /**
     * 从 Graph 的状态快照中提炼出便于前端和调试使用的响应对象。
     */
    private ApprovalWorkflowResponse buildResponse(String threadId) {
        StateSnapshot snapshot = approvalWorkflowGraph.getState(buildThreadConfig(threadId));
        Map<String, Object> stateData = new LinkedHashMap<>(snapshot.state().data());

        ApprovalWorkflowResponse response = new ApprovalWorkflowResponse();
        response.setThreadId(threadId);
        response.setCheckpointType(ThreadIdSupport.resolveMode(threadId));
        response.setCurrentNode(snapshot.node());
        response.setNextNode(snapshot.next());
        response.setInterrupted(snapshot.next() != null);
        response.setStatus(String.valueOf(stateData.getOrDefault(ApprovalWorkflowStateKeys.APPROVAL_STATUS, "UNKNOWN")));
        response.setState(stateData);
        response.setTimeline(extractTimeline(stateData));
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractTimeline(Map<String, Object> stateData) {
        Object timeline = stateData.get(ApprovalWorkflowStateKeys.TIMELINE);
        if (timeline instanceof List<?> timelineList) {
            return (List<String>) timelineList;
        }
        return new ArrayList<>();
    }

    private RunnableConfig buildThreadConfig(String threadId) {
        return RunnableConfig.builder()
                .threadId(threadId)
                .build();
    }
}
