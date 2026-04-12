package com.spring.quickstart.workflow.approval;

/**
 * 审批工作流状态字段常量。
 *
 * <p>Graph 的状态本质上是一个 Map，这里集中定义 key，避免后续在各个节点里写散乱的魔法字符串，
 * 也方便你学习时对照状态是如何在不同节点之间流转的。</p>
 */
public final class ApprovalWorkflowStateKeys {

    private ApprovalWorkflowStateKeys() {
    }

    public static final String REQUEST_NO = "requestNo";
    public static final String APPLICANT_NAME = "applicantName";
    public static final String APPLICANT_EMAIL = "applicantEmail";
    public static final String DEPARTMENT = "department";
    public static final String REASON = "reason";
    public static final String LEAVE_DAYS = "leaveDays";
    public static final String AMOUNT = "amount";
    public static final String APPROVER_NAME = "approverName";
    public static final String APPROVER_EMAIL = "approverEmail";
    public static final String APPROVAL_COMMENT = "approvalComment";
    public static final String APPROVAL_DECISION = "approvalDecision";
    public static final String APPROVAL_STATUS = "approvalStatus";
    public static final String APPROVAL_RESULT = "approvalResult";
    public static final String CURRENT_STAGE = "currentStage";
    public static final String TIMELINE = "timeline";
    public static final String MAIL_PUSH_STATUS = "mailPushStatus";
    public static final String RESULT_MAIL_STATUS = "resultMailStatus";
}
