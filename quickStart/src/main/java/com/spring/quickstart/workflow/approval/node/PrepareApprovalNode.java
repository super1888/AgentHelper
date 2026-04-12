package com.spring.quickstart.workflow.approval.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.spring.quickstart.workflow.approval.ApprovalStatus;
import com.spring.quickstart.workflow.approval.ApprovalWorkflowStateKeys;
import com.spring.quickstart.workflow.approval.support.ApprovalTimelineSupport;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第二步：生成审批摘要。
 *
 * <p>这里把推送邮件需要的信息提前整理好。真实项目里这一步也可以替换成 LLM 节点，
 * 让模型去生成更自然的审批摘要，但当前为了便于学习，先用稳定的 Java 逻辑实现。</p>
 */
public class PrepareApprovalNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String requestNo = state.value(ApprovalWorkflowStateKeys.REQUEST_NO, "");
        String applicantName = state.value(ApprovalWorkflowStateKeys.APPLICANT_NAME, "");
        String department = state.value(ApprovalWorkflowStateKeys.DEPARTMENT, "");
        String reason = state.value(ApprovalWorkflowStateKeys.REASON, "");
        Integer leaveDays = state.value(ApprovalWorkflowStateKeys.LEAVE_DAYS, 0);
        BigDecimal amount = state.value(ApprovalWorkflowStateKeys.AMOUNT, BigDecimal.ZERO);
        String approverName = state.value(ApprovalWorkflowStateKeys.APPROVER_NAME, "");

        String summary = """
                审批单号：%s
                申请人：%s
                部门：%s
                请假天数：%s
                金额：%s
                申请原因：%s
                待审批人：%s
                """.formatted(requestNo, applicantName, department, leaveDays, amount, reason, approverName);

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put("approvalSummary", summary);
        updates.put(ApprovalWorkflowStateKeys.CURRENT_STAGE, "审批摘要生成完成");
        updates.put(ApprovalWorkflowStateKeys.TIMELINE,
                ApprovalTimelineSupport.appendTimeline(state, "已生成审批摘要，准备推送给审批人"));
        updates.put(ApprovalWorkflowStateKeys.APPROVAL_STATUS, ApprovalStatus.WAITING_APPROVAL.name());
        return updates;
    }
}
