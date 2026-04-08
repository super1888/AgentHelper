package com.spring.ai.agent.factory;

import com.spring.ai.common.emun.AgentTypeEnum;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 统一工厂分发器（核心：自动找对应创建类）
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
@Component
@Slf4j
public class AgentFactory implements InitializingBean, ApplicationContextAware {

    private Map<AgentTypeEnum, AgentCreator> creatorMap = new ConcurrentHashMap<>(128);
    private ApplicationContext appContext;


    /**
     * 统一创建入口（根据类型自动找对应的Creator）
     */
    public Object createAgent(AgentTypeEnum type, Object dto) throws Exception {
        AgentCreator creator = creatorMap.get(type);
        if (creator == null) {
            throw new Exception("不支持的Agent类型：" + type);
        }
        return creator.createAgent(dto);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        appContext.getBeansOfType(AgentCreator.class).values()
                .forEach(strategy -> creatorMap.put(strategy.getAgentType(), strategy));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        appContext = applicationContext;
    }
}
