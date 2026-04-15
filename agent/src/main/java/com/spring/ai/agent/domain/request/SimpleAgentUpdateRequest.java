package com.spring.ai.agent.domain.request;

import java.util.List;
import lombok.Data;

/**
 * 更新 Agent 请求。
 */
@Data
public class SimpleAgentUpdateRequest {

    /**
     * Agent 名称。
     */
    private String agentName;

    /**
     * Agent 描述。
     */
    private String description;

    /**
     * 系统提示词。
     */
    private String systemPrompt;

    /**
     * 前端勾选的能力项。
     */
    private List<String> selectedCapabilities;
}
