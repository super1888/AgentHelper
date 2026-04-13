package com.spring.ai.agent.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleAgentCreateResponse {

    String agentId;

    String agentName;

    String description;

    List<String> selectedCapabilities;

    String websocketEndpoint;

    String websocketTopic;

    String websocketSendDestination;
}
