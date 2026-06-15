package com.spring.ai.codehelper.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 代码助手工具日志响应。
 */
@Data
@Builder
public class CodeHelperToolLogResponse {

    private Long logId;

    private String sessionId;

    private String toolName;

    private String riskLevel;

    private boolean success;

    private String requestJson;

    private String responseText;

    private Long durationMillis;

    private String errorMessage;

    private String createTime;
}
