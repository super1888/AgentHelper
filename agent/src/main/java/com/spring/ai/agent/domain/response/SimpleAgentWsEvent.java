package com.spring.ai.agent.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleAgentWsEvent {

    String agentId;

    String sessionId;

    String event;

    Object data;

    Long timestamp;
}
