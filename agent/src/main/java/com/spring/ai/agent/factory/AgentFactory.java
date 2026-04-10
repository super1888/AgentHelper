package com.spring.ai.agent.factory;

import com.spring.ai.common.enums.AgentTypeEnum;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Agent 工厂。
 *
 * <p>启动时自动收集所有 AgentCreator 实现，并按 AgentTypeEnum 建立映射关系。</p>
 */
@Component
@Slf4j
public class AgentFactory implements InitializingBean, ApplicationContextAware {

    private final Map<AgentTypeEnum, AgentCreator> creatorMap = new ConcurrentHashMap<>();
    private ApplicationContext appContext;

    /**
     * 统一 Agent 创建入口。
     *
     * @param type Agent 类型
     * @param dto  构建参数
     * @return 创建后的 Agent 实例
     */
    public Object createAgent(AgentTypeEnum type, Object dto) {
        AgentCreator creator = creatorMap.get(type);
        if (creator == null) {
            throw new BusinessException(ErrorCodeEnum.AGENT_CONFIG_ERROR, "不支持的 Agent 类型: " + type);
        }
        return creator.createAgent(dto);
    }

    @Override
    public void afterPropertiesSet() {
        appContext.getBeansOfType(AgentCreator.class).values()
                .forEach(strategy -> creatorMap.put(strategy.getAgentType(), strategy));
        log.info("Agent 创建器加载完成，数量={}", creatorMap.size());
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }
}
