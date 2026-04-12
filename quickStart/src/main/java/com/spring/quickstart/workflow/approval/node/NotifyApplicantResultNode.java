package com.spring.quickstart.workflow.approval.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.spring.quickstart.workflow.approval.ApprovalWorkflowStateKeys;
import com.spring.quickstart.workflow.approval.service.ApprovalMailService;
import com.spring.quickstart.workflow.approval.support.ApprovalTimelineSupport;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第五步：通知申请人审批结果。
 */
public class NotifyApplicantResultNode implements NodeAction {

    private final ApprovalMailService approvalMailService;
    private final boolean approved;

    public NotifyApplicantResultNode(ApprovalMailService approvalMailService, boolean approved) {
        this.approvalMailService = approvalMailService;
        this.approved = approved;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String applicantEmail = state.value(ApprovalWorkflowStateKeys.APPLICANT_EMAIL, "");
        String applicantName = state.value(ApprovalWorkflowStateKeys.APPLICANT_NAME, "");
        String requestNo = state.value(ApprovalWorkflowStateKeys.REQUEST_NO, "");
        String comment = state.value(ApprovalWorkflowStateKeys.APPROVAL_COMMENT, "未填写审批意见");

        String subject = approved ? "【审批通过】审批单 " + requestNo : "【审批驳回】审批单 " + requestNo;
        String content = approved
                ? """
                您好，%s：

                您提交的审批单 %s 已审批通过。
                审批意见：%s
                """.formatted(applicantName, requestNo, comment)
                : """
                您好，%s：

                您提交的审批单 %s 已被驳回。
                审批意见：%s
                """.formatted(applicantName, requestNo, comment);

        String mailStatus = approvalMailService.sendApprovalResult(applicantEmail, subject, content);

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(ApprovalWorkflowStateKeys.RESULT_MAIL_STATUS, mailStatus);
        updates.put(ApprovalWorkflowStateKeys.CURRENT_STAGE, approved ? "通过结果已通知申请人" : "驳回结果已通知申请人");
        updates.put(ApprovalWorkflowStateKeys.TIMELINE,
                ApprovalTimelineSupport.appendTimeline(
                        state,
                        approved ? "审批通过通知邮件已发送给申请人" : "审批驳回通知邮件已发送给申请人"));
        return updates;
    }
}
