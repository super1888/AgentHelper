package com.spring.ai.prompt.domain.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PromptTemplateBindDTO {

    Long promptTemplateId;

    String promptBindingType;

    String promptSourceType;

    String promptTemplateContent;

    String promptTemplatePath;
}
