package com.spring.ai.mcp.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * 文件用途：MCP 内置目录响应
 */
@Value
@Builder
public class McpCatalogResponse {

    String builtinServerKey;

    String serverName;

    String description;

    String serverType;

    String transportType;

    String riskLevel;

    Integer authRequired;

    List<String> exposedToolNames;

    String defaultRuntimeConfigJson;

    String defaultAuthConfigJson;

    String defaultTestPayloadJson;

    String toolPromptHint;
}
