package com.spring.ai.prompt.domain.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

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

    String effectiveSystemPrompt;
}
