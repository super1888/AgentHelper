package com.spring.ai.mcp.domain.request;

import lombok.Data;

/**
 * 文件用途：MCP 日志查询请求
 */
@Data
public class McpLogQueryRequest {

    private Long serverId;

    private String sourceType;

    private Integer successFlag;
}
