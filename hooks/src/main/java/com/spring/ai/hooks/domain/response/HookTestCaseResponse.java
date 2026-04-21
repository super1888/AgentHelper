package com.spring.ai.hooks.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：Hook 测试用例响应
 */
@Data
@Builder
public class HookTestCaseResponse {

    private Long id;
    private Long hookId;
    private String hookCode;
    private String caseName;
    private String inputPayloadJson;
    private String contextPayloadJson;
    private Integer expectedSuccess;
    private String expectedResponseContains;
    private Integer enabled;
    private String lastRunStatus;
    private Long lastRunDurationMs;
    private Long lastRunAt;
    private String lastResultJson;
}
