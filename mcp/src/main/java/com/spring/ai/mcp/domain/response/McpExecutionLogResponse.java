package com.spring.ai.mcp.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 文件用途：MCP 执行日志响应
 */
@Value
@Builder
public class McpExecutionLogResponse {

    Long logId;

    Long serverId;

    String serverCode;

    String serverName;

    String toolName;

    String sourceType;

    String executeStatus;

    Integer successFlag;

    String requestPayloadJson;

    String responsePayloadJson;

    String failureReason;

    Long elapsedMs;

    Long createTime;
}
