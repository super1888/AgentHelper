package com.spring.ai.agent.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleAgentRecoverResponse {

    String sessionId;

    String taskId;

    String taskStatus;

    String message;
}
