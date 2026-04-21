package com.spring.ai.agent.domain.response;

import com.spring.ai.agent.domain.dto.AgentPromptTemplateVariableDTO;
import lombok.Builder;
import lombok.Value;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class SimpleAgentVersionResponse {

    String versionId;

    Integer versionNo;

    String agentName;

    String description;

    String systemPrompt;

    List<String> selectedCapabilities;

    List<String> selectedHookCodes;

    String promptTemplateId;

    String promptTemplateCode;

    String promptTemplateName;

    String promptBindingType;

    String promptSourceType;

    String promptTemplatePath;

    String promptTemplateContent;

    List<AgentPromptTemplateVariableDTO> promptVariableDefinitions;

    Map<String, String> promptVariables;

    Boolean published;

    Long createTime;
}
