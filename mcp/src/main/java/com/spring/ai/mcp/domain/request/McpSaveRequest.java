package com.spring.ai.mcp.domain.request;

import java.util.List;
import lombok.Data;

/**
 * 文件用途：MCP 服务保存请求
 */
@Data
public class McpSaveRequest {

    private String serverCode;

    private String serverName;

    private String description;

    private String serverType;

    private String transportType;

    private String serverStatus;

    private String riskLevel;

    private Integer sortWeight;

    private Integer timeoutMs;

    private Integer authRequired;

    private String builtinServerKey;

    private String endpointUrl;

    private List<String> tags;

    private String runtimeConfigJson;

    private String authConfigJson;

    private String testPayloadJson;

    private String toolPromptHint;

    private String remark;
}
