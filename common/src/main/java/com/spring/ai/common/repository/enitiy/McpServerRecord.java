package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：MCP 服务管理主表实体
 * 核心职责：承载 MCP 服务目录、发布状态、接入方式和运行时配置归属信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_server_record")
public class McpServerRecord extends BaseEntity {

    @TableField("server_code")
    private String serverCode;

    @TableField("server_name")
    private String serverName;

    @TableField("description")
    private String description;

    @TableField("server_type")
    private String serverType;

    @TableField("transport_type")
    private String transportType;

    @TableField("server_status")
    private String serverStatus;

    @TableField("publish_status")
    private String publishStatus;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("sort_weight")
    private Integer sortWeight;

    @TableField("timeout_ms")
    private Integer timeoutMs;

    @TableField("auth_required")
    private Integer authRequired;

    @TableField("builtin_server_key")
    private String builtinServerKey;

    @TableField("endpoint_url")
    private String endpointUrl;

    @TableField("deleted_flag")
    private Integer deletedFlag;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("owner_user_name")
    private String ownerUserName;
}
