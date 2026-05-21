package com.spring.ai.flowable.domain.request;


import jakarta.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 流程审批动作请求参数。
 */
@Data
public class FlowableApprovalDecisionRequest {

    @NotNull
    private Boolean approved;

    @NotBlank
    private String operatorId;

    @NotBlank
    private String operatorName;

    @NotBlank
    private String comment;
}
