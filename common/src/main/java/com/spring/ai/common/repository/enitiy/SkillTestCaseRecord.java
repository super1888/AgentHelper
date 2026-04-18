package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Skill 测试用例记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("skill_test_case_record")
public class SkillTestCaseRecord extends BaseEntity {

    @TableField("skill_id")
    private Long skillId;

    @TableField("skill_code")
    private String skillCode;

    @TableField("case_name")
    private String caseName;

    @TableField("input_text")
    private String inputText;

    @TableField("slot_payload_json")
    private String slotPayloadJson;

    @TableField("expected_intent")
    private String expectedIntent;

    @TableField("expected_success")
    private Integer expectedSuccess;

    @TableField("expected_response_contains")
    private String expectedResponseContains;

    @TableField("channel_code")
    private String channelCode;

    @TableField("locale")
    private String locale;

    @TableField("enabled")
    private Integer enabled;

    @TableField("last_run_status")
    private String lastRunStatus;

    @TableField("last_run_duration_ms")
    private Long lastRunDurationMs;

    @TableField("last_run_at")
    private java.time.LocalDateTime lastRunAt;

    @TableField("last_result_json")
    private String lastResultJson;

    @TableField("tenant_id")
    private Long tenantId;
}
