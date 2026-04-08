package com.spring.ai.core.agent;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent.SequentialAgentBuilder;
import com.spring.ai.core.model.dto.AgentInfoDTO;
import com.spring.ai.core.model.dto.ParallelAgentDTO;
import com.spring.ai.core.model.dto.SequentialAgentDTO;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/23
 */
@Component
public class Agent {

    /**
     * 自定义agent
     *
     * @return
     */
    public ReactAgent customAgent(AgentInfoDTO agentInfoDTO) throws Exception {

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

        builder.enableLogging(agentInfoDTO.getEnableLogging());

        return builder.build();

    }

    /**
     * 创建多个agent工作流顺序执行agent
     *
     * @param dto
     * @return
     */
    public SequentialAgent creatSequentialAgent(SequentialAgentDTO dto) {

        // 如果DTO为空，直接返回空参构造的builder（或按你需求处理）
        if (dto == null) {
            throw new IllegalArgumentException("SequentialAgentDTO 不能为空");
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
