package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 任务实体。
 *
 * <p>记录一次用户输入触发的执行过程，以及失败恢复链路。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sy_agent_task")
public class SyAgentTask extends BaseEntity {

    /**
     * 任务业务编码。
     */
    @TableField("task_code")
    private String taskCode;

    /**
     * 来源失败任务 ID，首次执行时为空。
     */
    @TableField("source_task_id")
    private Long sourceTaskId;

    /**
     * 所属会话主键。
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 所属会话业务编码。
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
     * 所属用户 ID。
     */
    @TableField("owner_user_id")
    private Long ownerUserId;

    /**
     * 任务状态。
     */
    @TableField("task_status")
    private String taskStatus;

    /**
     * 请求消息。
     */
    @TableField("request_message")
    private String requestMessage;

    /**
     * 最终回复内容。
     */
    @TableField("response_message")
    private String responseMessage;

    /**
     * 错误信息。
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 当前恢复次数。
     */
    @TableField("retry_count")
    private Integer retryCount;
}
