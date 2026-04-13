package com.spring.ai.graph.node.sendEmailWorkflowNode;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.spring.ai.graph.service.ApprovalMailService;
import com.spring.ai.graph.stateKeys.ApprovalWorkflowStateKeys;
import com.spring.ai.graph.support.ApprovalTimelineSupport;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三步：给审批人发送审批推送。
 *
 * <p>这个节点执行完成后，Graph 会在下一个节点 {@code approvalDecision} 前中断，
 * 从而把“推送完成、等待人工审批”这件事情显式展示出来。</p>
 */
public class NotifyApproverNode implements NodeAction {

    private final ApprovalMailService approvalMailService;

    public NotifyApproverNode(ApprovalMailService approvalMailService) {
        this.approvalMailService = approvalMailService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String approverEmail = state.value(ApprovalWorkflowStateKeys.APPROVER_EMAIL, "");
        String approverName = state.value(ApprovalWorkflowStateKeys.APPROVER_NAME, "");
        String requestNo = state.value(ApprovalWorkflowStateKeys.REQUEST_NO, "");
        String approvalSummary = state.value("approvalSummary", "");

        String subject = "【待审批】审批单 " + requestNo;
        String content = """
                您好，%s：

                当前有一条新的审批待处理，请通过审批接口进行审批。

                审批摘要如下：
                %s
                """.formatted(approverName, approvalSummary);

        String mailStatus = approvalMailService.sendApprovalPush(approverEmail, subject, content);

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(ApprovalWorkflowStateKeys.MAIL_PUSH_STATUS, mailStatus);
        updates.put(ApprovalWorkflowStateKeys.CURRENT_STAGE, "审批推送完成，等待人工审批");
        updates.put(ApprovalWorkflowStateKeys.TIMELINE,
                ApprovalTimelineSupport.appendTimeline(state, "已向审批人发送待审批邮件，工作流将在审批节点前暂停"));
        return updates;
    }
}
