package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：工具管理主表实体
 * 核心功能：承载工具目录、发布状态、执行配置和归属信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_record")
public class ToolRecord extends BaseEntity {

    @TableField("tool_code")
    private String toolCode;

    @TableField("tool_name")
    private String toolName;

    @TableField("description")
    private String description;

    @TableField("tool_type")
    private String toolType;

    @TableField("tool_category")
    private String toolCategory;

    @TableField("source_type")
    private String sourceType;

    @TableField("tool_status")
    private String toolStatus;

    @TableField("publish_status")
    private String publishStatus;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("execution_mode")
    private String executionMode;

    @TableField("sort_weight")
    private Integer sortWeight;

    @TableField("timeout_ms")
    private Integer timeoutMs;

    @TableField("auth_required")
    private Integer authRequired;

    @TableField("builtin_tool_key")
    private String builtinToolKey;

    @TableField("endpoint_url")
    private String endpointUrl;

    @TableField("http_method")
    private String httpMethod;

    @TableField("deleted_flag")
    private Integer deletedFlag;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("owner_user_name")
    private String ownerUserName;
}
