package com.spring.ai.agent.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * AgentScope ReAct Agent 对话响应。
 *
 * @author zhouqi
 * @since 2026/6/26
 */
@Data
@Builder
public class AgentScopeReactChatResponse {

    /**
     * 实际使用的模型编码。
     */
    private String modelCode;

    /**
     * 原始用户输入。
     */
    private String userPrompt;

    /**
     * 最终回答。
     */
    private String finalAnswer;

    /**
     * 实际启用的工具名称。
     */
    private List<String> enabledTools;

    /**
     * 最大推理-行动轮次。
     */
    private Integer maxIters;

    /**
     * 调用耗时。
     */
    private Long costMs;
}
