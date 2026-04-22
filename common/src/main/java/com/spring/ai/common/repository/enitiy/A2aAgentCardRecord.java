package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：A2A Agent Card 主表实体
 * 核心职责：保存远程 Agent 的能力、连接、安全和运营治理元数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("a2a_agent_card_record")
public class A2aAgentCardRecord extends BaseEntity {

    @TableField("agent_code")
    private String agentCode;

    @TableField("agent_name")
    private String agentName;

    @TableField("description")
    private String description;

    @TableField("endpoint_url")
    private String endpointUrl;

    @TableField("protocol_version")
    private String protocolVersion;

    @TableField("transport_type")
    private String transportType;

    @TableField("auth_type")
    private String authType;

    @TableField("agent_status")
    private String agentStatus;

    @TableField("publish_status")
    private String publishStatus;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("trust_level")
    private String trustLevel;

    @TableField("owner_team")
    private String ownerTeam;

    @TableField("timeout_ms")
    private Integer timeoutMs;

    @TableField("rate_limit_qps")
    private Integer rateLimitQps;

    @TableField("success_rate_slo")
    private Integer successRateSlo;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("deleted_flag")
    private Integer deletedFlag;
}
