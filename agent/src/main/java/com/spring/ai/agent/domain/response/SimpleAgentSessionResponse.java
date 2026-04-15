package com.spring.ai.agent.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleAgentSessionResponse {

    String sessionId;

    String agentId;

    Integer agentVersionNo;

    Long agentVersionId;

    String sessionStatus;

    String connectionStatus;

    Long lastEventSequence;

    String websocketEndpoint;

    String websocketTopic;

    String websocketSendDestination;
}
