package com.spring.ai.agent.factory.impl;

import com.spring.ai.agent.factory.AbstractAgent;
import com.spring.ai.agent.factory.AgentCreator;
import com.spring.ai.common.enums.AgentTypeEnum;

/**
 * 自定义执行agent顺序
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
public class CustomizedAgent extends AbstractAgent implements AgentCreator {

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.CUSTOMIZED_AGENT;
    }

    @Override
    public Object createAgent(Object dto) {
        return super.createAgent(dto);
    }
}
