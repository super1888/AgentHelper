package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 会话表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sy_agent_session")
public class SyAgentSession extends BaseEntity {

    @TableField("session_code")
    private String sessionCode;

    @TableField("agent_id")
    private Long agentId;

    @TableField("agent_code")
    private String agentCode;

    @TableField("agent_version_id")
    private Long agentVersionId;

    @TableField("agent_version_no")
    private Integer agentVersionNo;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("owner_user_name")
    private String ownerUserName;

    @TableField("session_status")
    private String sessionStatus;

    @TableField("connection_status")
    private String connectionStatus;

    @TableField("last_event_sequence")
    private Long lastEventSequence;

    @TableField("last_user_message")
    private String lastUserMessage;

    @TableField("last_assistant_message")
    private String lastAssistantMessage;

    @TableField("last_connected_time")
    private LocalDateTime lastConnectedTime;

    @TableField("last_disconnected_time")
    private LocalDateTime lastDisconnectedTime;
}
