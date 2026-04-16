package com.spring.ai.agent.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPromptTemplateVariableDTO {

    private String variableName;

    private Boolean required;

    private String defaultValue;

    private String description;
}
