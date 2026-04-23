package com.spring.ai.agent.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleAgentSummaryResponse {

    String agentId;

    String agentName;

    String description;

    String agentType;

    String agentStatus;

    Integer currentVersionNo;

    Integer publishedVersionNo;

    String ownerUserName;

    String modelCode;

    String modelName;

    String providerName;
}
