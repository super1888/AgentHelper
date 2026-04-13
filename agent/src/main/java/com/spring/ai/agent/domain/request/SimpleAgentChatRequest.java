package com.spring.ai.agent.domain.request;

import lombok.Data;

@Data
public class SimpleAgentChatRequest {

    private String agentId;

    private String sessionId;

    private String message;
}
