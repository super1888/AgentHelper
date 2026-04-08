package com.spring.ai.agent.factory.impl;

import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent.SequentialAgentBuilder;
import com.spring.ai.agent.domian.dto.ParallelAgentDTO;
import com.spring.ai.agent.domian.dto.SequentialAgentDTO;
import com.spring.ai.agent.factory.AbstractAgent;
import com.spring.ai.agent.factory.AgentCreator;
import com.spring.ai.common.emun.AgentTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 工作流并行执行Agent
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@Component
@Slf4j
public class CreatParallelAgent extends AbstractAgent implements AgentCreator {

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.PARALLEL_AGENT;
    }

    @Override
    public Object createAgent(Object dto) throws Exception {
        ParallelAgentDTO agentInfoDTO = (ParallelAgentDTO) dto;
        return creatParallelAgent(agentInfoDTO);
    }

    /**
     * 创建多个agent工作流并行执行agent
     *
     * @param dto
     * @return
     */
    public ParallelAgent creatParallelAgent(ParallelAgentDTO dto) {

        // DTO 不能为空
        if (dto == null) {
            throw new IllegalArgumentException("ParallelAgentDTO 不能为空");
        }

        ParallelAgent.ParallelAgentBuilder builder = ParallelAgent.builder();

        // 基础字段（有文本才复制）
        if (StringUtils.hasText(dto.getName())) {
            builder.name(dto.getName());
        }
        if (StringUtils.hasText(dto.getDescription())) {
            builder.description(dto.getDescription());
        }

        // 对象字段（非空才复制）
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

        // ParallelAgent 独有字段
        if (dto.getMergeStrategy() != null) {
            builder.mergeStrategy(dto.getMergeStrategy());
        }
        if (dto.getMaxConcurrency() != null) {
            builder.maxConcurrency(dto.getMaxConcurrency());
        }
        if (StringUtils.hasText(dto.getMergeOutputKey())) {
            builder.mergeOutputKey(dto.getMergeOutputKey());
        }

        // 构造并返回
        return builder.build();
    }


}
