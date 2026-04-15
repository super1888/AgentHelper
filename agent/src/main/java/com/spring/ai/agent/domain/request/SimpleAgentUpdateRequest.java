package com.spring.ai.agent.domain.request;

import java.util.List;
import lombok.Data;

/**
 * 更新 Agent 请求。
 */
@Data
public class SimpleAgentUpdateRequest {

    /**
     * 更新后的 Agent 名称。
     */
    private String agentName;

    /**
     * 更新后的描述。
     */
    private String description;

    /**
     * 更新后的系统提示词。
     */
    private String systemPrompt;

    /**
     * 更新后的能力选择。
     */
    private List<String> selectedCapabilities;
}
