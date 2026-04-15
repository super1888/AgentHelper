package com.spring.ai.agent.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleAgentReconnectResponse {

    SimpleAgentSessionResponse session;

    List<SimpleAgentWsEvent> missedEvents;
}
