package com.spring.ai.codehelper.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 代码助手工具执行响应。
 */
@Data
@Builder
public class CodeHelperToolExecutionResponse {

    private String sessionId;

    private String toolName;

    private boolean success;

    private String riskLevel;

    private String message;

    private String output;

    private Long durationMillis;
}
