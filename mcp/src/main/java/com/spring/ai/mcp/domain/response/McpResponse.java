package com.spring.ai.mcp.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * 文件用途：MCP 服务管理响应
 */
@Value
@Builder
public class McpResponse {

    Long serverId;

    String serverCode;

    String serverName;

    String description;

    String serverType;

    String transportType;

    String serverStatus;

    String publishStatus;

    String riskLevel;

    Integer sortWeight;

    Integer timeoutMs;

    Integer authRequired;

    String builtinServerKey;

    String endpointUrl;

    List<String> tags;

    String runtimeConfigJson;

    String authConfigJson;

    String testPayloadJson;

    String toolPromptHint;

    String remark;

    Integer logCount;
}
