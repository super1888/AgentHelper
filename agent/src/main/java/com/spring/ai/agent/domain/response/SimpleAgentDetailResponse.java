package com.spring.ai.agent.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleAgentDetailResponse {

    String agentId;

    String agentName;

    String description;

    String agentType;

    String agentStatus;

    Integer currentVersionNo;

    Integer publishedVersionNo;

    Long ownerUserId;

    String ownerUserName;

    List<SimpleAgentVersionResponse> versions;
}
