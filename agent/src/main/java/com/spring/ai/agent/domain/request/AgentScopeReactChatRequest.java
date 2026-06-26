package com.spring.ai.agent.domain.request;

import java.util.List;
import lombok.Data;

/**
 * AgentScope ReAct Agent 对话请求。
 *
 * @author zhouqi
 * @since 2026/6/26
 */
@Data
public class AgentScopeReactChatRequest {

    /**
     * 使用的模型编码，当前仅支持 DashScope 模型配置。
     */
    private String modelCode;

    /**
     * 用户输入的问题或任务。
     */
    private String userPrompt;

    /**
     * 系统提示词，不传时使用默认企业级 ReAct 提示词。
     */
    private String systemPrompt;

    /**
     * 最大推理-行动轮次。
     */
    private Integer maxIters;

    /**
     * 本次允许调用的工具名称白名单。
     */
    private List<String> enabledTools;
}
