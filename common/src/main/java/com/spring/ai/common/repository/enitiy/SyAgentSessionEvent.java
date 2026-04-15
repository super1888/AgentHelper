package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 会话事件表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sy_agent_session_event")
public class SyAgentSessionEvent extends BaseEntity {

    @TableField("session_id")
    private Long sessionId;

    @TableField("session_code")
    private String sessionCode;

    @TableField("agent_id")
    private Long agentId;

    @TableField("agent_version_id")
    private Long agentVersionId;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("task_id")
    private Long taskId;

    @TableField("event_sequence")
    private Long eventSequence;

    @TableField("event_type")
    private String eventType;

    @TableField("event_body")
    private String eventBody;

    @TableField("replayable")
    private Integer replayable;
}
