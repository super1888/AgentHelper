package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：Hook 与 Agent 绑定实体
 * 核心职责：保存 Hook 的目标 Agent、模型、环境等绑定规则
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hook_agent_binding_record")
public class HookAgentBindingRecord extends BaseEntity {

    @TableField("hook_id")
    private Long hookId;

    @TableField("hook_code")
    private String hookCode;

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
