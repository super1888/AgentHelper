package com.spring.quickstart.workflow.approval.domain.request;

/**
 * 人工审批动作请求。
 */
public class ApprovalDecisionRequest {

    private Boolean approved;
    private String comment;

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
