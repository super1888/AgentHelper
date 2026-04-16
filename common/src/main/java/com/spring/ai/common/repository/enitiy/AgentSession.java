package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 会话实体。
 *
 * <p>会话在创建时固定绑定某个 Agent 版本，后续事件流和任务恢复都以此为准。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_session")
public class AgentSession extends BaseEntity {

    /**
     * 会话业务编码。
     */
    @TableField("session_code")
    private String sessionCode;

    /**
     * 关联 Agent 主档 ID。
     */
    @TableField("agent_id")
    private Long agentId;

    /**
     * 关联 Agent 业务编码。
     */
    @TableField("agent_code")
    private String agentCode;

    /**
     * 绑定的 Agent 版本 ID。
     */
    @TableField("agent_version_id")
    private Long agentVersionId;

    /**
     * 绑定的 Agent 版本号。
     */
    @TableField("agent_version_no")
    private Integer agentVersionNo;

    /**
     * 所属租户 ID。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 所属用户 ID。
     */
    @TableField("owner_user_id")
    private Long ownerUserId;

    /**
     * 所属用户名。
     */
    @TableField("owner_user_name")
    private String ownerUserName;

    /**
     * 会话状态。
     */
    @TableField("session_status")
    private String sessionStatus;

    /**
     * WebSocket 连接状态。
     */
    @TableField("connection_status")
    private String connectionStatus;

    /**
     * 最近一次已分配事件序号。
     */
    @TableField("last_event_sequence")
    private Long lastEventSequence;

    /**
     * 最近一次用户输入。
     */
    @TableField("last_user_message")
    private String lastUserMessage;

    /**
     * 最近一次助手回复。
     */
    @TableField("last_assistant_message")
    private String lastAssistantMessage;

    /**
     * 最近一次连接时间。
     */
    @TableField("last_connected_time")
    private LocalDateTime lastConnectedTime;

    /**
     * 最近一次断开时间。
     */
    @TableField("last_disconnected_time")
    private LocalDateTime lastDisconnectedTime;
}
