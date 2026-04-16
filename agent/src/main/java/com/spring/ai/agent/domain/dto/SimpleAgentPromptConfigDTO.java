package com.spring.ai.agent.domain.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleAgentPromptConfigDTO {

    private Long promptTemplateId;

    private String promptTemplateCode;

    private String promptTemplateName;

    private String promptBindingType;

    private String promptSourceType;

    private String promptTemplatePath;

    private String promptTemplateContent;

    private List<AgentPromptTemplateVariableDTO> promptVariableDefinitions;

    private Map<String, String> promptVariables;
}
