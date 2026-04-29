package com.spring.ai.mcp.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件用途：MCP 服务扩展配置 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerExtDTO {

    private List<String> tags;

    private String runtimeConfigJson;

    private String authConfigJson;

    private String testPayloadJson;

    private String toolPromptHint;
}
