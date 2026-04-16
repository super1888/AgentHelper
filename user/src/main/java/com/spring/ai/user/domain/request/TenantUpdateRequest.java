package com.spring.ai.user.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新租户请求。
 */
@Data
public class TenantUpdateRequest {

    @NotBlank(message = "租户编码不能为空")
    @Size(max = 64, message = "租户编码长度不能超过64位")
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 128, message = "租户名称长度不能超过128位")
    private String tenantName;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "联系电话格式不正确")
    private String contactPhone;

    @Size(max = 64, message = "联系人长度不能超过64位")
    private String contactName;

    @Size(max = 500, message = "租户描述长度不能超过500位")
    private String description;

    private Integer status;
}
