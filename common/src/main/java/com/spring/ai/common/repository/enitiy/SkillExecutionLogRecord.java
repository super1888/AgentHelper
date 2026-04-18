package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Skill 执行日志记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("skill_execution_log_record")
public class SkillExecutionLogRecord extends BaseEntity {

    @TableField("skill_id")
    private Long skillId;

    @TableField("skill_code")
    private String skillCode;

    @TableField("skill_name")
    private String skillName;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private Long sourceId;

    @TableField("trace_id")
    private String traceId;

    @TableField("session_code")
    private String sessionCode;

    @TableField("channel_code")
    private String channelCode;

    @TableField("locale")
    private String locale;

    @TableField("input_text")
    private String inputText;

    @TableField("matched_intent")
    private String matchedIntent;

    @TableField("confidence_score")
    private Double confidenceScore;

    @TableField("slot_payload_json")
    private String slotPayloadJson;

    @TableField("context_payload_json")
    private String contextPayloadJson;

    @TableField("request_payload_json")
    private String requestPayloadJson;

    @TableField("response_payload_json")
    private String responsePayloadJson;

    @TableField("trace_payload_json")
    private String tracePayloadJson;

    @TableField("execute_status")
    private String executeStatus;

    @TableField("success_flag")
    private Integer successFlag;

    @TableField("elapsed_ms")
    private Long elapsedMs;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("satisfaction_level")
    private Integer satisfactionLevel;

    @TableField("operator_user_id")
    private Long operatorUserId;

    @TableField("operator_user_name")
    private String operatorUserName;
}
