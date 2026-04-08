package com.spring.ai.agent.domian.dto;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 在监督者模式中，使用大语言模型（LLM）作为监督者，动态决定将任务路由到哪个子Agent，并支持多步骤循环路由。与 LlmRoutingAgent 不同，SupervisorAgent 支持子Agent执行完成后返回监督者，监督者可以根据执行结果继续路由到其他Agent或完成任务。
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class SupervisorAgentDTO extends FlowAgentDTO {

    // ==================== SupervisorAgent 独有字段 ====================
    /**
     * 对话模型
     */
    private ChatModel chatModel;

    /**
     * 系统提示词 已弃用
     */
    private String systemPrompt;

    /**
     * 执行指令 已弃用
     */
    private String instruction;

    /**
     * 主智能体名称
     */
    private ReactAgent mainAgent;

}
