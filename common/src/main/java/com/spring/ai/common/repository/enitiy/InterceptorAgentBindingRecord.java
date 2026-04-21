package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：Interceptor 绑定实体
 * 核心职责：定义拦截器与 Agent、模型、环境的绑定范围
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interceptor_agent_binding_record")
public class InterceptorAgentBindingRecord extends BaseEntity {

    @TableField("interceptor_id")
    private Long interceptorId;

    @TableField("interceptor_code")
    private String interceptorCode;

    @TableField("binding_name")
    private String bindingName;

    @TableField("binding_scope")
    private String bindingScope;

    @TableField("target_agent_code")
    private String targetAgentCode;

    @TableField("target_model_code")
    private String targetModelCode;

    @TableField("environment_code")
    private String environmentCode;

    @TableField("priority_no")
    private Integer priorityNo;

    @TableField("enabled")
    private Integer enabled;

    @TableField("tenant_id")
    private Long tenantId;
}
