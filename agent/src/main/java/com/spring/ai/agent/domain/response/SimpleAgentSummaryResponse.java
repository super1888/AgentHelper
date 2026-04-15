package com.spring.ai.agent.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * Agent 列表摘要。
 */
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
}
