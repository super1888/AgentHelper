package com.spring.ai.agent.factory.impl;

import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent.SequentialAgentBuilder;
import com.spring.ai.agent.domain.dto.SequentialAgentDTO;
import com.spring.ai.agent.factory.AbstractAgent;
import com.spring.ai.agent.factory.AgentCreator;
import com.spring.ai.common.enums.AgentTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 工作流顺序执行Agent
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@Component
@Slf4j
public class CreatSequentialAgent extends AbstractAgent implements AgentCreator {

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.SEQUENTIAL;
    }

    @Override
    public Object createAgent(Object dto) {
        SequentialAgentDTO agentInfoDTO = (SequentialAgentDTO) dto;
        return creatSequentialAgent(agentInfoDTO);
    }

    /**
     * 创建多个agent
     *
     * @param dto
     * @return
     */
    public SequentialAgent creatSequentialAgent(SequentialAgentDTO dto) {

        // 如果DTO为空，直接返回空参构造的builder（或按你需求处理）
        if (dto == null) {
            throw new IllegalArgumentException("串行智能体配置不能为空");
        }

        SequentialAgentBuilder builder = SequentialAgent.builder();

        // 名称：有文本就复制
        if (StringUtils.hasText(dto.getName())) {
            builder.name(dto.getName());
        }

        // 描述：有文本就复制
        if (StringUtils.hasText(dto.getDescription())) {
            builder.description(dto.getDescription());
        }

        // 编译配置：非空就复制
        if (dto.getCompileConfig() != null) {
            builder.compileConfig(dto.getCompileConfig());
        }

        // 子智能体：非空就复制
        if (dto.getSubAgents() != null) {
            builder.subAgents(dto.getSubAgents());
        }

        // 状态序列化器：非空就复制
        if (dto.getStateSerializer() != null) {
            builder.stateSerializer(dto.getStateSerializer());
        }

        // 执行器：非空就复制
        if (dto.getExecutor() != null) {
            builder.executor(dto.getExecutor());
        }

        // 钩子函数：非空就复制
        if (dto.getHooks() != null) {
            builder.hooks(dto.getHooks());
        }

        return builder.build();

    }

}
