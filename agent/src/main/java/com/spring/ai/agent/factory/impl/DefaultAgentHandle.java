package com.spring.ai.agent.factory.impl;

import com.spring.ai.agent.factory.AbstractAgent;
import com.spring.ai.agent.factory.AgentCreator;
import com.spring.ai.common.emun.AgentTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认工厂
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */

@Component
@Slf4j
public class DefaultAgentHandle extends AbstractAgent implements AgentCreator {


    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.DEFAULT;
    }

    @Override
    public Object createAgent(Object dto) throws Exception {

        return super.createAgent(dto);
    }
}
