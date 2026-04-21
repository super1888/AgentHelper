package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：Hook 测试用例实体
 * 核心职责：保存 Hook 的调试回归用例
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hook_test_case_record")
public class HookTestCaseRecord extends BaseEntity {

    @TableField("hook_id")
    private Long hookId;

    @TableField("hook_code")
    private String hookCode;

    @TableField("case_name")
    private String caseName;

    @TableField("input_payload_json")
    private String inputPayloadJson;

    @TableField("context_payload_json")
    private String contextPayloadJson;

    @TableField("expected_success")
    private Integer expectedSuccess;

    @TableField("expected_response_contains")
    private String expectedResponseContains;

    @TableField("enabled")
    private Integer enabled;

    @TableField("last_run_status")
    private String lastRunStatus;

    @TableField("last_run_duration_ms")
    private Long lastRunDurationMs;

    @TableField("last_run_at")
    private LocalDateTime lastRunAt;

    @TableField("last_result_json")
    private String lastResultJson;

    @TableField("tenant_id")
    private Long tenantId;
}
