package com.spring.ai.agent.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleAgentVersionResponse {

    Long versionId;

    Integer versionNo;

    String agentName;

    String description;

    String systemPrompt;

    List<String> selectedCapabilities;

    Boolean published;

    Long createTime;
}
