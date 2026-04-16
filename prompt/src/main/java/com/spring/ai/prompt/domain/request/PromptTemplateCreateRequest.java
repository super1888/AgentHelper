package com.spring.ai.prompt.domain.request;

import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import java.util.List;
import lombok.Data;

@Data
public class PromptTemplateCreateRequest {

    private String templateCode;

    private String templateName;

    private String description;

    private String sourceType;

    private String templateContent;

    private String sourcePath;

    private List<PromptTemplateVariableDTO> variableDefinitions;
}
