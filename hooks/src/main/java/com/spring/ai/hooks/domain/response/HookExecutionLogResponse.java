package com.spring.ai.hooks.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：Hook 执行日志响应
 */
@Data
@Builder
public class HookExecutionLogResponse {

    private Long id;
    private Long hookId;
    private String hookCode;
    private String hookName;
    private String sourceType;
    private Long sourceId;
    private String traceId;
    private String agentCode;
    private String sessionCode;
    private String requestPayloadJson;
    private String contextPayloadJson;
    private String responsePayloadJson;
    private String executeStatus;
    private Integer successFlag;
    private Long elapsedMs;
    private String failureReason;
    private String operatorUserName;
    private Long createTime;
}
