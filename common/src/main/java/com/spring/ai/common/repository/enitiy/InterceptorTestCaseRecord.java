package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件用途：Interceptor 测试用例实体
 * 核心职责：保存拦截器调试与回归验证的测试输入和最近执行结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interceptor_test_case_record")
public class InterceptorTestCaseRecord extends BaseEntity {

    @TableField("interceptor_id")
    private Long interceptorId;

    @TableField("interceptor_code")
    private String interceptorCode;

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

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("last_run_status")
    private String lastRunStatus;

    @TableField("last_run_duration_ms")
    private Long lastRunDurationMs;

    @TableField("last_run_at")
    private LocalDateTime lastRunAt;

    @TableField("last_result_json")
    private String lastResultJson;
}
