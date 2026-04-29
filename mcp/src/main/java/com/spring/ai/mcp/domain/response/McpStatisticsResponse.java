package com.spring.ai.mcp.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 文件用途：MCP 统计响应
 */
@Value
@Builder
public class McpStatisticsResponse {

    Integer totalCount;

    Integer enabledCount;

    Integer publishedCount;

    Integer builtinCount;

    Integer remoteCount;

    Integer highRiskCount;

    Integer totalLogCount;

    Integer successLogCount;

    Integer failureLogCount;
}
