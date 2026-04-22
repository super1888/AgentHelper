package com.spring.ai.a2a.domain.request;

import java.util.Map;
import lombok.Data;

@Data
public class A2aDispatchRequest {
    private String sourceAgentCode;
    private String targetAgentCode;
    private String taskType;
    private Map<String, Object> payload;
}
