package com.spring.quickstart.workflow.approval.config;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.spring.quickstart.workflow.approval.ApprovalWorkflowStateKeys;
import com.spring.quickstart.workflow.approval.node.ApprovalDecisionNode;
import com.spring.quickstart.workflow.approval.node.NotifyApplicantResultNode;
import com.spring.quickstart.workflow.approval.node.NotifyApproverNode;
import com.spring.quickstart.workflow.approval.node.PrepareApprovalNode;
import com.spring.quickstart.workflow.approval.node.ValidateApprovalRequestNode;
import com.spring.quickstart.workflow.approval.service.ApprovalMailService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 审批 Graph 配置。
 *
 * <p>这个配置类就是你要学习的重点：如何把文档里的 StateGraph、interruptBefore、MemorySaver
 * 组合成一个真正可对外提供服务的业务工作流。</p>
 */
@Configuration
public class ApprovalWorkflowGraphConfig {

    /**
     * 注册内存检查点保存器。
     *
     * <p>当前示例为了便于快速跑通，先用 MemorySaver。后续如果你希望跨进程持久化，
     * 可以把它切换为 RedisSaver、MysqlSaver 等。</p>
     */
    @Bean
    public MemorySaver approvalWorkflowMemorySaver() {
        return new MemorySaver();
    }

    /**
     * 编译审批 Graph。
     *
     * <p>这里显式声明在 {@code approvalDecision} 节点前中断，这样当流程跑到“等待人工审批”时，
     * Graph 会先保存当前状态，然后暂停，直到外部接口调用 {@code updateState()} 写回审批结果。</p>
     */
    @Bean("approvalWorkflowGraph")
    public CompiledGraph approvalWorkflowGraph(
            ApprovalMailService approvalMailService,
            MemorySaver approvalWorkflowMemorySaver) throws Exception {

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
                .saverConfig(SaverConfig.builder().register(approvalWorkflowMemorySaver).build())
                // 显示声明 终端
                .interruptBefore("approvalDecision")
                .build());
    }
}
