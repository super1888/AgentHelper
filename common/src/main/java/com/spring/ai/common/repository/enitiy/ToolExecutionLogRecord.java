package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：工具执行日志表实体
 * 核心功能：记录工具调试与运行阶段的输入输出和执行结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_execution_log_record")
public class ToolExecutionLogRecord extends BaseEntity {

    @TableField("tool_id")
    private Long toolId;

    @TableField("tool_code")
    private String toolCode;

    @TableField("tool_name")
    private String toolName;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("source_type")
    private String sourceType;

    @TableField("request_payload_json")
    private String requestPayloadJson;

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
