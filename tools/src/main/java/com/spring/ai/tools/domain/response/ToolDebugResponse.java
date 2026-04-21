package com.spring.ai.tools.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：工具在线调试响应对象
 */
@Data
@Builder
public class ToolDebugResponse {

    private Long toolId;

    private String toolCode;

    private String toolName;

    private Integer successFlag;

    private String responseText;

    private String failureReason;

    private Long elapsedMs;

    private String requestPayloadJson;

    private String responsePayloadJson;
}
