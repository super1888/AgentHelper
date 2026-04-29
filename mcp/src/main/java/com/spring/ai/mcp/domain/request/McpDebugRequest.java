package com.spring.ai.mcp.domain.request;

import lombok.Data;

/**
 * 文件用途：MCP 服务调试请求
 */
@Data
public class McpDebugRequest {

    private Long serverId;

    private String requestPayloadJson;

    private String sourceType;
}
