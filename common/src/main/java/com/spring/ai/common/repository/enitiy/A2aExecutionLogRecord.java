package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：A2A 执行日志实体
 * 核心职责：保存跨 Agent 调用链路的审计与观测日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("a2a_execution_log_record")
public class A2aExecutionLogRecord extends BaseEntity {

    @TableField("task_code")
    private String taskCode;

    @TableField("trace_id")
    private String traceId;

    @TableField("source_agent_code")
    private String sourceAgentCode;

    @TableField("target_agent_code")
    private String targetAgentCode;

    @TableField("route_code")
    private String routeCode;

    @TableField("event_type")
    private String eventType;

    @TableField("execute_status")
    private String executeStatus;

    @TableField("attempt_no")
    private Integer attemptNo;

    @TableField("retry_index")
    private Integer retryIndex;

    @TableField("success_flag")
    private Integer successFlag;

    @TableField("elapsed_ms")
    private Long elapsedMs;

    @TableField("request_payload_json")
    private String requestPayloadJson;

    @TableField("response_payload_json")
    private String responsePayloadJson;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("tenant_id")
    private Long tenantId;
}
