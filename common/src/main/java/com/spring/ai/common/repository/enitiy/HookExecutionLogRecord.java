package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：Hook 执行日志实体
 * 核心职责：保存 Hook 调试、测试和运行阶段日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hook_execution_log_record")
public class HookExecutionLogRecord extends BaseEntity {

    @TableField("hook_id")
    private Long hookId;

    @TableField("hook_code")
    private String hookCode;

    @TableField("hook_name")
    private String hookName;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private Long sourceId;

    @TableField("trace_id")
    private String traceId;

    @TableField("agent_code")
    private String agentCode;

    @TableField("session_code")
    private String sessionCode;

    @TableField("request_payload_json")
    private String requestPayloadJson;

    @TableField("context_payload_json")
    private String contextPayloadJson;

    @TableField("response_payload_json")
    private String responsePayloadJson;

    @TableField("execute_status")
    private String executeStatus;

    @TableField("success_flag")
    private Integer successFlag;

    @TableField("elapsed_ms")
    private Long elapsedMs;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("operator_user_id")
    private Long operatorUserId;

    @TableField("operator_user_name")
    private String operatorUserName;
}
