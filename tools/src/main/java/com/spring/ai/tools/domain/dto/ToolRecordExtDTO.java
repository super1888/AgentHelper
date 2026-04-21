package com.spring.ai.tools.domain.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：工具扩展配置 DTO
 * 核心功能：承载工具管理页中的 JSON 配置和标签信息
 */
@Data
@Builder
public class ToolRecordExtDTO {

    private List<String> tags;

    private String requestSchemaJson;

    private String authConfigJson;

    private String runtimeConfigJson;

    private String testPayloadJson;
}
