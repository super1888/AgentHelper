package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 任务表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sy_agent_task")
public class SyAgentTask extends BaseEntity {

    @TableField("task_code")
    private String taskCode;

    @TableField("source_task_id")
    private Long sourceTaskId;

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

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("task_status")
    private String taskStatus;

    @TableField("request_message")
    private String requestMessage;

    @TableField("response_message")
    private String responseMessage;

    @TableField("error_message")
    private String errorMessage;

    @TableField("retry_count")
    private Integer retryCount;
}
