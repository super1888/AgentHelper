package com.spring.ai.agent.domain.request;

import java.util.List;
import lombok.Data;

/**
 * 创建 Agent 请求。
 */
@Data
public class SimpleAgentCreateRequest {

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

    /**
     * Agent 类型，当前仅支持 REACT。
     */
    private String agentType;
}
