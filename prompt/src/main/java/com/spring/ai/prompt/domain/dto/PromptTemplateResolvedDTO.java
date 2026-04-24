package com.spring.ai.prompt.domain.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * 文件用途：封装提示词模板解析后的结果。
 * 核心功能：返回模板元信息、变量绑定结果和最终生效的系统提示词。
 */
@Value
@Builder
public class PromptTemplateResolvedDTO {

    Long promptTemplateId;

    String promptTemplateCode;

    String promptTemplateName;

    String promptBindingType;

    String promptSourceType;

    String promptTemplatePath;

    List<PromptTemplateVariableDTO> variableDefinitions;

    Map<String, String> promptVariables;

    PromptTemplateEnterpriseConfigDTO enterpriseConfig;

    String effectiveSystemPrompt;
}
