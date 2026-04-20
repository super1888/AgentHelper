package com.spring.ai.prompt.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件用途：描述模板变量的定义信息。
 * 核心功能：约束变量名称、必填性、默认值和业务说明。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateVariableDTO {

    private String variableName;

    private Boolean required;

    private String defaultValue;

    private String description;
}
