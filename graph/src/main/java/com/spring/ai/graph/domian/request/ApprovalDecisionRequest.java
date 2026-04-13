package com.spring.ai.graph.domian.request;

import lombok.Data;

/**
 * 人工审批动作请求。
 * 该类用于封装人工审批操作所需的数据，包括审批结果和审批意见。
 */
@Data  // 使用Lombok注解自动生成getter、setter、toString等方法
public class ApprovalDecisionRequest {

    private Boolean approved;  // 审批结果，true表示通过，false表示拒绝
    private String comment;    // 审批意见，用于记录审批人的具体意见或备注

}
