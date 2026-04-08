package com.spring.ai.agent.domian.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.ai.chat.model.ChatModel;

/**
 * LLM分析输入并决定最合适的子Agent
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class LlmRoutingAgentDTO extends FlowAgentDTO {

    // ==================== LlmRoutingAgent 独有字段 ====================
    /**
     * 对话模型（ChatModel）
     */
    private ChatModel chatModel;

    /**
     * 降级/备用智能体名称
     */
    private String fallbackAgent;

    /**
     * 系统提示词 用于设置路由决策的系统提示，会替换默认的系统提示。你可以通过它提供详细的决策规则和上下文
     */
    private String systemPrompt;

    /**
     * 路由指令 用于设置路由决策的用户指令，会作为 UserMessage 添加到消息列表中。你可以通过它提供额外的上下文信息或特定的路由指导：
     */
    private String instruction;

}
