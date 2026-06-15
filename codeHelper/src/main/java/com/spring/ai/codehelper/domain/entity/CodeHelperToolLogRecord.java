package com.spring.ai.codehelper.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码助手工具执行日志实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("code_helper_tool_log")
public class CodeHelperToolLogRecord extends BaseEntity {

    @TableField("session_code")
    private String sessionCode;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("tool_name")
    private String toolName;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("success_flag")
    private Integer successFlag;

    @TableField("request_json")
    private String requestJson;

    @TableField("response_text")
    private String responseText;

    @TableField("duration_millis")
    private Long durationMillis;

    @TableField("error_message")
    private String errorMessage;
}
