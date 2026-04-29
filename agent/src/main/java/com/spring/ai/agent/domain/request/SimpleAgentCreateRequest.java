package com.spring.ai.agent.domain.request;

import com.spring.ai.agent.domain.dto.SimpleAgentPromptConfigDTO;
import java.util.List;
import lombok.Data;

@Data
public class SimpleAgentCreateRequest {

    private String agentName;

    private String description;

    private String systemPrompt;

    private List<String> selectedCapabilities;

    private List<String> selectedHookCodes;

    private List<String> selectedMcpServerIds;

    private String agentType;

    private SimpleAgentPromptConfigDTO promptConfig;

    private String modelConfigCode;
}
