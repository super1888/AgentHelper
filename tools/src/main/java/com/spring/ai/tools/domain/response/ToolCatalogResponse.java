package com.spring.ai.tools.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：内置工具目录响应对象
 */
@Data
@Builder
public class ToolCatalogResponse {

    private String toolKey;

    private String toolName;

    private String description;

    private String toolType;

    private String toolCategory;

    private String sourceType;

    private List<String> tags;

    private String defaultRequestSchemaJson;

    private String defaultRuntimeConfigJson;

    private String defaultTestPayloadJson;
}
