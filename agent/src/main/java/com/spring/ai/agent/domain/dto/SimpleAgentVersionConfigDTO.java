package com.spring.ai.agent.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleAgentVersionConfigDTO {

    private String agentName;

    private String description;

    private String systemPrompt;

    private List<String> selectedCapabilities;

    private List<String> selectedHookCodes;

    private SimpleAgentPromptConfigDTO promptConfig;

    private SimpleAgentModelBindingDTO modelBinding;
}
