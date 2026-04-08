package com.spring.ai.agent.domian.dto;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent.MergeStrategy;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;

import java.util.List;
import java.util.concurrent.Executor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 并行执行agent
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class ParallelAgentDTO  extends FlowAgentDTO{

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
