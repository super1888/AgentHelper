package com.spring.ai.tools.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：工具执行日志响应对象
 */
@Data
@Builder
public class ToolExecutionLogResponse {

    private Long id;

    private Long toolId;

    private String toolCode;

    private String toolName;

    private String sourceType;

    private String requestPayloadJson;

    private String responsePayloadJson;

    private String executeStatus;

    private Integer successFlag;

    private Long elapsedMs;

    private String failureReason;

    private String operatorUserName;

    private Long createTime;
}
