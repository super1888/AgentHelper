package com.spring.ai.a2a.domain.request;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class A2aAgentCardSaveRequest {
    private String agentCode;
    private String agentName;
    private String description;
    private String endpointUrl;
    private String protocolVersion;
    private String transportType;
    private String authType;
    private String agentStatus;
    private String riskLevel;
    private String trustLevel;
    private String ownerTeam;
    private Integer timeoutMs;
    private Integer retryTimes;
    private Integer rateLimitQps;
    private Integer successRateSlo;
    private List<String> capabilities;
    private List<String> inputModes;
    private List<String> outputModes;
    private Map<String, Object> authConfig;
    private Map<String, Object> metadata;
    private String remark;
}
