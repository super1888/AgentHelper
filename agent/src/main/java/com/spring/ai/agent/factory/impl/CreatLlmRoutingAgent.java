package com.spring.ai.agent.factory.impl;

import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.spring.ai.agent.domain.dto.LlmRoutingAgentDTO;
import com.spring.ai.agent.factory.AbstractAgent;
import com.spring.ai.agent.factory.AgentCreator;
import com.spring.ai.common.enums.AgentTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 在路由模式中，使用大语言模型（LLM）动态决定将请求路由到哪个子Agent。这种模式非常适合需要智能选择不同专家Agent的场景。 1. 提供清晰明确的Agent描述 2. 明确Agent的职责边界 3. 使用不同领域的Agent避免重叠
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@Component
@Slf4j
public class CreatLlmRoutingAgent extends AbstractAgent implements AgentCreator {

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.LLM_ROUTING_AGENT;
    }

    @Override
    public Object createAgent(Object dto) {
        LlmRoutingAgentDTO agentInfoDTO = (LlmRoutingAgentDTO) dto;
        return creatLlmRoutingAgent(agentInfoDTO);
    }


    public LlmRoutingAgent creatLlmRoutingAgent(LlmRoutingAgentDTO dto) {

        // DTO 不能为空
        if (dto == null) {
            throw new IllegalArgumentException("LlmRoutingAgentDTO 不能为空");
        }

        LlmRoutingAgent.LlmRoutingAgentBuilder builder = LlmRoutingAgent.builder();

        // ==================== 父类通用字段 ====================
        if (StringUtils.hasText(dto.getName())) {
            builder.name(dto.getName());
        }
        if (StringUtils.hasText(dto.getDescription())) {
            builder.description(dto.getDescription());
        }
        if (dto.getCompileConfig() != null) {
            builder.compileConfig(dto.getCompileConfig());
        }
        if (dto.getSubAgents() != null) {
            builder.subAgents(dto.getSubAgents());
        }
        if (dto.getStateSerializer() != null) {
            builder.stateSerializer(dto.getStateSerializer());
        }
        if (dto.getExecutor() != null) {
            builder.executor(dto.getExecutor());
        }
        if (dto.getHooks() != null) {
            builder.hooks(dto.getHooks());
        }

        // ==================== LLM 路由独有字段 ====================
        if (dto.getChatModel() != null) {
            builder.model(dto.getChatModel());
        }
        if (StringUtils.hasText(dto.getFallbackAgent())) {
            builder.fallbackAgent(dto.getFallbackAgent());
        }
        if (StringUtils.hasText(dto.getSystemPrompt())) {
            builder.systemPrompt(dto.getSystemPrompt());
        }
        if (StringUtils.hasText(dto.getInstruction())) {
            builder.instruction(dto.getInstruction());
        }

        // 构造并返回
        return builder.build();
    }


}
