package com.spring.quickstart.workflow.approval.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.spring.quickstart.workflow.approval.ApprovalStatus;
import com.spring.quickstart.workflow.approval.ApprovalWorkflowStateKeys;
import com.spring.quickstart.workflow.approval.support.ApprovalTimelineSupport;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第四步：消费人工审批结果。
 *
 * <p>这个节点本身不负责“等待用户输入”，等待动作由 compile 时的 {@code interruptBefore("approvalDecision")}
 * 完成。控制层收到人工审批请求后，会先调用 {@code updateState()} 把审批结果写入状态，再继续运行到这里。</p>
 */
public class ApprovalDecisionNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Boolean approved = state.value(ApprovalWorkflowStateKeys.APPROVAL_DECISION, Boolean.class)
                .orElseThrow(() -> new IllegalArgumentException("审批结果不能为空"));
        String comment = state.value(ApprovalWorkflowStateKeys.APPROVAL_COMMENT, "未填写审批意见");

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(ApprovalWorkflowStateKeys.APPROVAL_RESULT, approved ? "APPROVED" : "REJECTED");
        updates.put(ApprovalWorkflowStateKeys.APPROVAL_STATUS,
                approved ? ApprovalStatus.APPROVED.name() : ApprovalStatus.REJECTED.name());
        updates.put(ApprovalWorkflowStateKeys.CURRENT_STAGE, approved ? "审批通过" : "审批驳回");
        updates.put(ApprovalWorkflowStateKeys.TIMELINE,
                ApprovalTimelineSupport.appendTimeline(
                        state,
                        approved ? "审批人已通过审批，意见：" + comment : "审批人已驳回审批，意见：" + comment));
        return updates;
    }
}
