package com.spring.ai.mcp.domain.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件用途：天气 MCP 运行时配置 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherMcpRuntimeConfigDTO {

    private String baseUrl;

    private String path;

    private String locationParamName;

    private String unitsParamName;

    private String languageParamName;

    private String defaultUnits;

    private String responsePath;

    private Map<String, String> staticQueryParams;
}
