package com.spring.ai.prompt.domain.request;

import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import java.util.List;
import lombok.Data;

@Data
public class PromptTemplateUpdateRequest {

    private String templateName;

    private String description;

    private String sourceType;

    private String templateContent;

    private String sourcePath;

    private String templateStatus;

    private List<PromptTemplateVariableDTO> variableDefinitions;
}
