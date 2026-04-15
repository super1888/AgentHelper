package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 会话事件实体。
 *
 * <p>用于保存会话中的事件流，支持断线补发与历史追溯。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sy_agent_session_event")
public class SyAgentSessionEvent extends BaseEntity {

    /**
     * 关联会话主键。
     */
    @TableField("session_id")
    private Long sessionId;

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
     * 关联 Agent 版本 ID。
     */
    @TableField("agent_version_id")
    private Long agentVersionId;

    /**
     * 所属租户 ID。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 关联任务 ID。
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 会话内自增事件序号。
     */
    @TableField("event_sequence")
    private Long eventSequence;

    /**
     * 事件类型。
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 事件内容。
     */
    @TableField("event_body")
    private String eventBody;

    /**
     * 是否允许断线补发。
     */
    @TableField("replayable")
    private Integer replayable;
}
