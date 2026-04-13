package com.spring.ai.agent.domain.request;

import java.util.List;
import lombok.Data;

@Data
public class SimpleAgentCreateRequest {

    private String agentName;

    private String description;

    private String systemPrompt;

    /**
     * 模拟用户在前端勾选的配置项。
     */
    private List<String> selectedCapabilities;
}
