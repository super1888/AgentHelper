package com.spring.ai.agent.factory;

import com.spring.ai.common.emun.AgentTypeEnum;

/**
 * agent工厂父类
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
public interface AgentCreator {

    /**
     * 当前Creator支持的Agent类型
     */
    AgentTypeEnum getAgentType();

    /**
     * 创建Agent
     */
    Object createAgent(Object dto) throws Exception;

}
