package com.spring.ai.a2a.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class A2aStatisticsResponse {
    Integer agentCount;
    Integer publishedAgentCount;
    Integer routeCount;
    Integer taskCount;
    Integer successTaskCount;
    Integer failedTaskCount;
    Integer logCount;
}
