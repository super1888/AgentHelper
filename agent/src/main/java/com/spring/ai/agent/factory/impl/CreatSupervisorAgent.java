package com.spring.ai.agent.factory.impl;

import com.alibaba.cloud.ai.graph.agent.flow.agent.SupervisorAgent;
import com.spring.ai.agent.domain.dto.SupervisorAgentDTO;
import com.spring.ai.agent.factory.AbstractAgent;
import com.spring.ai.agent.factory.AgentCreator;
import com.spring.ai.common.enums.AgentTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 监督者模式中，使用大语言模型（LLM）作为监督者，动态决定将任务路由到哪个子Agent，并支持多步骤循环路由。与 LlmRoutingAgent 不同，SupervisorAgent 支持子Agent执行完成后返回监督者，监督者可以根据执行结果继续路由到其他Agent或完成任务。
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@Component
@Slf4j
public class CreatSupervisorAgent extends AbstractAgent implements AgentCreator {

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.SUPERVISOR_AGENT;
    }

    @Override
    public Object createAgent(Object dto) {
        SupervisorAgentDTO agentInfoDTO = (SupervisorAgentDTO) dto;
        return creatSupervisorAgent(agentInfoDTO);
    }

    /**
     * 监督者模式中，使用大语言模型（LLM）作为监督者，动态决定将任务路由到哪个子Agent，并支持多步骤循环路由。与 LlmRoutingAgent 不同，SupervisorAgent 支持子Agent执行完成后返回监督者，监督者可以根据执行结果继续路由到其他Agent或完成任务。
     *
     * @param dto
     * @return
     */
    public SupervisorAgent creatSupervisorAgent(SupervisorAgentDTO dto) {

        // DTO 不能为空
        if (dto == null) {
            throw new IllegalArgumentException("主管智能体配置不能为空");
        }

        SupervisorAgent.SupervisorAgentBuilder builder = SupervisorAgent.builder();

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

        // ==================== 管理者智能体独有字段 ====================
        if (dto.getChatModel() != null) {
            builder.model(dto.getChatModel());
        }
        if (StringUtils.hasText(dto.getSystemPrompt())) {
            builder.systemPrompt(dto.getSystemPrompt());
        }
        if (StringUtils.hasText(dto.getInstruction())) {
            builder.instruction(dto.getInstruction());
        }
        if (dto.getMainAgent() != null) {
            builder.mainAgent(dto.getMainAgent());
        }

        // 构造并返回
        return builder.build();
    }

}
