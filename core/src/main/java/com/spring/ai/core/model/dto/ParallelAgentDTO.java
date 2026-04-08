package com.spring.ai.core.model.dto;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent.MergeStrategy;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;

import java.util.List;
import java.util.concurrent.Executor;
import lombok.Builder;
import lombok.Data;

/**
 * 并行执行agent
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@Data
@Builder
public class ParallelAgentDTO {

    /**
     * 智能体名称
     */
    private String name;

    /**
     * 智能体描述
     */
    private String description;

    /**
     * 编译配置
     */
    private CompileConfig compileConfig;

    /**
     * 子智能体列表
     */
    private List<Agent> subAgents;

    /**
     * 状态序列化器
     */
    private StateSerializer stateSerializer;

    /**
     * 线程执行器
     */
    private Executor executor;

    /**
     * 钩子函数
     */
    private List<Hook> hooks;

    /**
     * 结果合并策略（并行执行后如何合并多个子节点输出）
     */
    private MergeStrategy mergeStrategy;

    /**
     * 最大并发数
     */
    private Integer maxConcurrency;

    /**
     * 合并结果输出KEY（指定输出到哪个字段）
     */
    private String mergeOutputKey;

}
