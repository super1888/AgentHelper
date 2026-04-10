package com.spring.ai.agent.factory.impl;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.spring.ai.agent.domian.dto.AgentInfoDTO;
import com.spring.ai.agent.factory.AbstractAgent;
import com.spring.ai.agent.factory.AgentCreator;
import com.spring.ai.common.enums.AgentTypeEnum;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * ReactAgent 创建器。
 *
 * <p>负责根据 AgentInfoDTO 组装标准的 ReactAgent。</p>
 */
@Component
@Slf4j
public class CustomAgent extends AbstractAgent implements AgentCreator {

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.REACT;
    }

    @Override
    public Object createAgent(Object dto) {
        AgentInfoDTO agentInfoDTO = (AgentInfoDTO) dto;
        return buildReactAgent(agentInfoDTO);
    }

    /**
     * 创建 ReactAgent。
     *
     * @param agentInfoDTO Agent 配置对象
     * @return ReactAgent
     */
    private ReactAgent buildReactAgent(AgentInfoDTO agentInfoDTO) {
        if (agentInfoDTO == null) {
            throw new BusinessException(ErrorCodeEnum.AGENT_CONFIG_ERROR, "AgentInfoDTO 不能为空");
        }
        if (!StringUtils.hasText(agentInfoDTO.getAgentName()) || agentInfoDTO.getModel() == null) {
            throw new BusinessException(ErrorCodeEnum.AGENT_CONFIG_ERROR, "agentName 和 model 不能为空");
        }

        Builder builder = ReactAgent.builder();

        if (StringUtils.hasText(agentInfoDTO.getDescription())) {
            builder.description(agentInfoDTO.getDescription());
        }
        if (StringUtils.hasText(agentInfoDTO.getOutputKey())) {
            builder.outputKey(agentInfoDTO.getOutputKey());
        }
        if (StringUtils.hasText(agentInfoDTO.getInstruction())) {
            builder.instruction(agentInfoDTO.getInstruction());
        }
        if (agentInfoDTO.getReturnReasoningContents() != null) {
            builder.returnReasoningContents(agentInfoDTO.getReturnReasoningContents());
        }
        if (agentInfoDTO.getIncludeContents() != null) {
            builder.includeContents(agentInfoDTO.getIncludeContents());
        }

        builder.name(agentInfoDTO.getAgentName());
        builder.model(agentInfoDTO.getModel());

        if (agentInfoDTO.getMethodTools() != null) {
            builder.methodTools(agentInfoDTO.getMethodTools().toArray());
        }
        if (agentInfoDTO.getInterceptors() != null) {
            builder.interceptors(agentInfoDTO.getInterceptors());
        }
        if (agentInfoDTO.getOutputTypeClass() != null) {
            builder.outputType(agentInfoDTO.getOutputTypeClass());
        }

        if (agentInfoDTO.getOutputSchemaClass() != null) {
            BeanOutputConverter<?> outputConverter = new BeanOutputConverter<>(agentInfoDTO.getOutputSchemaClass());
            builder.outputSchema(outputConverter.getFormat());
        }
        else if (StringUtils.hasText(agentInfoDTO.getOutputSchemaJson())) {
            builder.outputSchema(agentInfoDTO.getOutputSchemaJson());
        }

        if (agentInfoDTO.getInputSchemaClass() != null) {
            BeanOutputConverter<?> inputConverter = new BeanOutputConverter<>(agentInfoDTO.getInputSchemaClass());
            builder.inputSchema(inputConverter.getFormat());
        }
        else if (StringUtils.hasText(agentInfoDTO.getInputSchemaJson())) {
            builder.inputSchema(agentInfoDTO.getInputSchemaJson());
        }

        if (Boolean.TRUE.equals(agentInfoDTO.getIsMemory())) {
            builder.saver(agentInfoDTO.getMemorySaver());
        }
        if (agentInfoDTO.getHooks() != null) {
            builder.hooks(agentInfoDTO.getHooks());
        }
        if (agentInfoDTO.getTools() != null) {
            builder.tools(agentInfoDTO.getTools());
        }
        if (agentInfoDTO.getEnableLogging() != null) {
            builder.enableLogging(agentInfoDTO.getEnableLogging());
        }

        return builder.build();
    }
}
