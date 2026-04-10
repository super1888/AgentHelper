package com.spring.ai.agent.factory;

import com.spring.ai.common.enums.AgentTypeEnum;

/**
 * Agent 创建器接口。
 *
 * <p>不同类型的 Agent 通过各自的创建器实现统一接入 AgentFactory。</p>
 */
public interface AgentCreator {

    /**
     * 获取当前创建器支持的 Agent 类型。
     *
     * @return Agent 类型
     */
    AgentTypeEnum getAgentType();

    /**
     * 根据传入 DTO 创建 Agent 实例。
     *
     * @param dto Agent 构建参数
     * @return 创建完成的 Agent 对象
     */
    Object createAgent(Object dto);
}
