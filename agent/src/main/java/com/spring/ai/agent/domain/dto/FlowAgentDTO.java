package com.spring.ai.agent.domain.dto;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * agentFLow 父类
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@NoArgsConstructor
@Data
public class FlowAgentDTO {

    /**
     * 智能体名称（唯一标识/业务名称）
     */
    private String name;

    /**
     * 智能体描述（功能说明、用途描述）
     */
    private String description;

    /**
     * 编译配置（代码/逻辑编译相关的配置参数）
     */
    private CompileConfig compileConfig;

    /**
     * 子智能体列表（当前智能体包含的下级子代理集合）
     */
    private List<Agent> subAgents;

    /**
     * 状态序列化器（用于智能体状态的序列化/反序列化操作）
     */
    private StateSerializer stateSerializer;

    /**
     * 线程执行器（用于执行异步任务、调度任务的线程池）
     */
    private Executor executor;

    /**
     * 钩子函数集合（智能体生命周期钩子，如初始化、执行前后回调）
     */
    private List<Hook> hooks;
}
