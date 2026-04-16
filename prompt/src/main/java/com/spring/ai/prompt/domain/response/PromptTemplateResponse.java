package com.spring.ai.prompt.domain.response;

import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PromptTemplateResponse {

    Long id;

    String templateCode;

    String templateName;

    String description;

    String templateType;

    String sourceType;

    String templateContent;

    String sourcePath;

    String templateStatus;

    Long ownerUserId;

    String ownerUserName;

    List<PromptTemplateVariableDTO> variableDefinitions;

    Long createTime;

    Long updateTime;
}
