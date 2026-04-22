package com.spring.ai.a2a.domain.response;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class A2aAgentCardResponse {
    Long id;
    String agentCode;
    String agentName;
    String description;
    String endpointUrl;
    String protocolVersion;
    String transportType;
    String authType;
    String agentStatus;
    String publishStatus;
    String riskLevel;
    String trustLevel;
    String ownerTeam;
    Integer timeoutMs;
    Integer retryTimes;
    Integer rateLimitQps;
    Integer successRateSlo;
    List<String> capabilities;
    List<String> inputModes;
    List<String> outputModes;
    Map<String, Object> authConfig;
    Map<String, Object> metadata;
    String remark;
    Long createTime;
    Long updateTime;
}
