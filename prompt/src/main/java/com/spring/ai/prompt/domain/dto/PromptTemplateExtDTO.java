package com.spring.ai.prompt.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件用途：定义模板扩展字段的统一存储结构。
 * 核心功能：兼容历史变量数组结构，并扩展企业配置对象存储。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateExtDTO {

    private List<PromptTemplateVariableDTO> variableDefinitions;

    private PromptTemplateEnterpriseConfigDTO enterpriseConfig;
}
