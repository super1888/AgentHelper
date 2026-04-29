package com.spring.ai.mcp.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件用途：数据库 MCP 运行时配置 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseMcpRuntimeConfigDTO {

    private String dataSourceBeanName;

    private Integer defaultLimit;

    private Integer maxLimit;

    private Integer queryTimeoutSeconds;

    private Integer allowSchemaInspect;

    private List<String> allowedTables;
}
