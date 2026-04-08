package com.spring.ai.agent.factory.impl;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.spring.ai.agent.domian.dto.AgentInfoDTO;
import com.spring.ai.agent.factory.AbstractAgent;
import com.spring.ai.agent.factory.AgentCreator;
import com.spring.ai.common.emun.AgentTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 自定义Agent工厂
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@Component
@Slf4j
public class CustomAgent extends AbstractAgent implements AgentCreator {

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.REACT;
    }

    @Override
    public Object createAgent(Object dto) throws Exception {
        AgentInfoDTO agentInfoDTO = (AgentInfoDTO) dto;
        return buildReactAgent(agentInfoDTO);
    }

    /**
     * 自定义agent
     *
     * @return
     */
    private ReactAgent buildReactAgent(AgentInfoDTO agentInfoDTO) throws Exception {

        Builder builder = ReactAgent.builder();

        if (agentInfoDTO.getAgentName() == null || agentInfoDTO.getModel() == null) {
            throw new Exception("agentName不能为空");
        }

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
            String format = outputConverter.getFormat();
            builder.outputSchema(format);
        } else if (StringUtils.hasText(agentInfoDTO.getOutputSchemaJson())) {
            builder.outputSchema(agentInfoDTO.getOutputSchemaJson());
        }

        if (agentInfoDTO.getIsMemory() != null && agentInfoDTO.getIsMemory()) {
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
