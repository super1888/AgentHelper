package com.spring.ai.agent.factory;

import com.spring.ai.common.emun.AgentTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 抽象工厂 存储重复数据
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@Component
@Slf4j
public abstract class AbstractAgent implements AgentCreator {

    @Override
    public AgentTypeEnum getAgentType() {
        return null;
    }

    @Override
    public Object createAgent(Object dto) throws Exception {
        log.info("未创建Agent！");
        return null;
    }
}
