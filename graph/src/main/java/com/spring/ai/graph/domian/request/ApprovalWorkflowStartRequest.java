package com.spring.ai.graph.domian.request;

import com.spring.ai.common.enums.graph.ApprovalWorkflowCheckpointModeEnum;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 发起审批请求。
 *
 * <p>这里故意把字段设计得直白一些，便于你直接通过 Postman 调接口学习整条链路。</p>
 */
@Data
public class ApprovalWorkflowStartRequest {

    // 审批请求编号，用于唯一标识一个审批请求
    private String requestNo;
    // 申请人姓名，记录发起审批请求的用户姓名
    private String applicantName;
    // 申请人邮箱，用于通知和联系申请人
    private String applicantEmail;
    // 申请人所在部门，用于审批流程中的部门审核环节
    private String department;
    // 审批原因，详细说明发起审批的理由
    private String reason;
    // 请假天数，当审批类型为请假时使用，表示请假的天数
    private Integer leaveDays;
    // 审批金额，当审批类型为费用报销时使用，表示申请报销的金额
    private BigDecimal amount;
    // 审批人姓名，指定负责审批该请求的人员姓名
    private String approverName;
    // 审批人邮箱，用于通知和联系审批人
    private String approverEmail;
    // 审批检查点模式，指定审批流程的类型和方式
    private ApprovalWorkflowCheckpointModeEnum checkpointType;

}
