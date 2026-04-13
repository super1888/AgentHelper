package com.spring.ai.graph.config.approvalWorkFlowConfig;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import com.spring.ai.graph.node.sendEmailWorkflowNode.ApprovalDecisionNode;
import com.spring.ai.graph.node.sendEmailWorkflowNode.NotifyApplicantResultNode;
import com.spring.ai.graph.node.sendEmailWorkflowNode.NotifyApproverNode;
import com.spring.ai.graph.node.sendEmailWorkflowNode.PrepareApprovalNode;
import com.spring.ai.graph.node.sendEmailWorkflowNode.ValidateApprovalRequestNode;
import com.spring.ai.graph.service.ApprovalMailService;
import com.spring.ai.graph.stateKeys.ApprovalWorkflowStateKeys;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 审批 Graph 配置。
 *
 * <p>这里不再直接绑定某一种 checkpoint saver，而是统一依赖动态 saver，
 * 这样后续切换 Memory 或 Redis 只需要改配置，不需要改 Graph 代码。</p>
 */
@Configuration
public class ApprovalWorkflowGraphConfig {

    @Resource
    private ApprovalMailService approvalMailService;

    @Resource(name = "approvalWorkflowCheckpointSaver")
    private BaseCheckpointSaver approvalWorkflowCheckpointSaver;

    @Bean("approvalWorkflowGraph")
    public CompiledGraph approvalWorkflowGraph() throws Exception {
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put(ApprovalWorkflowStateKeys.REQUEST_NO, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.APPLICANT_NAME, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.APPLICANT_EMAIL, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.DEPARTMENT, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.REASON, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.LEAVE_DAYS, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.AMOUNT, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.APPROVER_NAME, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.APPROVER_EMAIL, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.APPROVAL_DECISION, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.APPROVAL_COMMENT, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.APPROVAL_STATUS, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.APPROVAL_RESULT, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.CURRENT_STAGE, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.TIMELINE, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.MAIL_PUSH_STATUS, new ReplaceStrategy());
            strategies.put(ApprovalWorkflowStateKeys.RESULT_MAIL_STATUS, new ReplaceStrategy());
            strategies.put("approvalSummary", new ReplaceStrategy());
            return strategies;
        };

        StateGraph graph = new StateGraph("approval-workflow", keyStrategyFactory);
        graph.addNode("validateRequest", node_async(new ValidateApprovalRequestNode()));
        graph.addNode("prepareApproval", node_async(new PrepareApprovalNode()));
        graph.addNode("notifyApprover", node_async(new NotifyApproverNode(approvalMailService)));
        graph.addNode("approvalDecision", node_async(new ApprovalDecisionNode()));
        graph.addNode("notifyApprovedApplicant", node_async(new NotifyApplicantResultNode(approvalMailService, true)));
        graph.addNode("notifyRejectedApplicant", node_async(new NotifyApplicantResultNode(approvalMailService, false)));

        graph.addEdge(START, "validateRequest");
        graph.addEdge("validateRequest", "prepareApproval");
        graph.addEdge("prepareApproval", "notifyApprover");
        graph.addEdge("notifyApprover", "approvalDecision");
        graph.addConditionalEdges(
                "approvalDecision",
                edge_async(state -> state.value(ApprovalWorkflowStateKeys.APPROVAL_RESULT, "REJECTED")),
                Map.of(
                        "APPROVED", "notifyApprovedApplicant",
                        "REJECTED", "notifyRejectedApplicant"));
        graph.addEdge("notifyApprovedApplicant", END);
        graph.addEdge("notifyRejectedApplicant", END);

        return graph.compile(CompileConfig.builder()
                .saverConfig(SaverConfig.builder().register(approvalWorkflowCheckpointSaver).build())
                .interruptBefore("approvalDecision")
                .build());
    }
}
