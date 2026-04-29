package com.spring.ai.mcp.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 文件用途：MCP 调试响应
 */
@Value
@Builder
public class McpDebugResponse {

    Long serverId;

    String serverCode;

    String serverName;

    Integer successFlag;

    String requestPayloadJson;

    String responsePayloadJson;

    String failureReason;

    Long elapsedMs;
}
