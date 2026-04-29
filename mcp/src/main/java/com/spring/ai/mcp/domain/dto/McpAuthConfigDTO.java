package com.spring.ai.mcp.domain.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件用途：MCP 服务鉴权配置 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpAuthConfigDTO {

    private String authType;

    private String apiKey;

    private String headerName;

    private String queryParamName;

    private Map<String, String> extraHeaders;
}
