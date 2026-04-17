package com.spring.ai.agent.factory;

import com.spring.ai.common.enums.AgentTypeEnum;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Agent 创建器抽象父类。
 *
 * <p>统一收敛默认行为，避免子类未实现时返回 null，降低调用方排查成本。</p>
 */
@Component
@Slf4j
public abstract class AbstractAgent implements AgentCreator {

    @Override
    public AgentTypeEnum getAgentType() {
        throw new UnsupportedOperationException("子类必须实现获取智能体类型的方法");
    }

    @Override
    public Object createAgent(Object dto) {
        log.warn("Agent 创建器尚未实现，creator={}", this.getClass().getName());
        throw new BusinessException(ErrorCodeEnum.AGENT_CONFIG_ERROR, "当前智能体创建器尚未实现");
    }
}
