package com.spring.ai.mcp.application.model;

import lombok.Builder;
import lombok.Value;

/**
 * 文件用途：MCP 工具执行结果
 */
@Value
@Builder
public class McpInvocationResult {

    String toolName;

    String responsePayloadJson;

    String displayText;
}
