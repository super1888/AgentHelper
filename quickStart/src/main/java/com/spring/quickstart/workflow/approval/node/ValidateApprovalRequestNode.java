package com.spring.quickstart.workflow.approval.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.spring.quickstart.workflow.approval.ApprovalStatus;
import com.spring.quickstart.workflow.approval.ApprovalWorkflowStateKeys;
import com.spring.quickstart.workflow.approval.support.ApprovalTimelineSupport;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 第一步：校验并初始化审批单。
 *
 * <p>这个节点只做“输入兜底”和“状态初始化”，不直接发送邮件，也不做审批判定。
 * 这是 Graph 文档里强调的“单节点只负责一件事”的思路。</p>
 */
public class ValidateApprovalRequestNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String applicantName = requireText(state, ApprovalWorkflowStateKeys.APPLICANT_NAME, "申请人姓名不能为空");
        String applicantEmail = requireText(state, ApprovalWorkflowStateKeys.APPLICANT_EMAIL, "申请人邮箱不能为空");
        String reason = requireText(state, ApprovalWorkflowStateKeys.REASON, "审批原因不能为空");
        String approverName = requireText(state, ApprovalWorkflowStateKeys.APPROVER_NAME, "审批人姓名不能为空");
        String approverEmail = requireText(state, ApprovalWorkflowStateKeys.APPROVER_EMAIL, "审批人邮箱不能为空");

        Integer leaveDays = state.value(ApprovalWorkflowStateKeys.LEAVE_DAYS, 0);
        if (leaveDays == null || leaveDays <= 0) {
            throw new IllegalArgumentException("请假天数必须大于 0");
        }

        BigDecimal amount = state.value(ApprovalWorkflowStateKeys.AMOUNT, BigDecimal.ZERO);
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负数");
        }

        String requestNo = state.value(ApprovalWorkflowStateKeys.REQUEST_NO,
                "REQ-" + System.currentTimeMillis());
        String department = state.value(ApprovalWorkflowStateKeys.DEPARTMENT, "未填写部门");

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(ApprovalWorkflowStateKeys.REQUEST_NO, requestNo);
        updates.put(ApprovalWorkflowStateKeys.APPLICANT_NAME, applicantName);
        updates.put(ApprovalWorkflowStateKeys.APPLICANT_EMAIL, applicantEmail);
        updates.put(ApprovalWorkflowStateKeys.REASON, reason);
        updates.put(ApprovalWorkflowStateKeys.APPROVER_NAME, approverName);
        updates.put(ApprovalWorkflowStateKeys.APPROVER_EMAIL, approverEmail);
        updates.put(ApprovalWorkflowStateKeys.DEPARTMENT, department);
        updates.put(ApprovalWorkflowStateKeys.LEAVE_DAYS, leaveDays);
        updates.put(ApprovalWorkflowStateKeys.AMOUNT, amount == null ? BigDecimal.ZERO : amount);
        updates.put(ApprovalWorkflowStateKeys.CURRENT_STAGE, "表单校验完成");
        updates.put(ApprovalWorkflowStateKeys.APPROVAL_STATUS, ApprovalStatus.SUBMITTED.name());
        updates.put(ApprovalWorkflowStateKeys.TIMELINE,
                ApprovalTimelineSupport.appendTimeline(state, "校验申请单成功，准备进入审批推送节点"));
        return updates;
    }

    private String requireText(OverAllState state, String key, String message) {
        String value = state.value(key, "");
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
