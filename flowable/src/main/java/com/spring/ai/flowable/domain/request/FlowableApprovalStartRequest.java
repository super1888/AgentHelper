package com.spring.ai.flowable.domain.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 发起流程审批的请求参数。
 */
@Data
public class FlowableApprovalStartRequest {

    @NotBlank
    private String businessKey;

    @NotBlank
    private String requestNo;

    @NotBlank
    private String title;

    @NotBlank
    private String applicantId;

    @NotBlank
    private String applicantName;

    @NotBlank
    private String approverUserId;

    private String approverName;

    @NotBlank
    private String department;

    @NotBlank
    private String reason;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal amount;

    @Min(1)
    private Integer leaveDays;
}
