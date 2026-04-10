package com.spring.ai.hooks.factory;

import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.spring.ai.common.enums.HookTypeEnum;
import com.spring.ai.hooks.domain.dto.HumanInTheLoopHookDTO;
import com.spring.ai.hooks.domain.dto.ModelCallLimitHookDTO;
import com.spring.ai.hooks.domain.dto.PIIDetectionHookDTO;
import com.spring.ai.hooks.domain.dto.SkillsAgentHookDTO;
import com.spring.ai.hooks.domain.dto.SummarizationHookDTO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Hook 工厂分发器。
 *
 * <p>内部实现方式与 AgentFactory 保持一致，通过 HookTypeEnum 自动分发到不同创建器。</p>
 */
@Component
public class HookFactory implements InitializingBean, ApplicationContextAware {

    private final Map<HookTypeEnum, HookCreator> creatorMap = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;

    @SuppressWarnings("unchecked")
    public <T> T createHook(HookTypeEnum type, Object dto) {
        HookCreator creator = creatorMap.get(type);
        if (creator == null) {
            throw new IllegalArgumentException("不支持的 Hook 类型: " + type);
        }
        return (T) creator.create(dto);
    }

    public SummarizationHook creatSummarizationHook(SummarizationHookDTO dto) {
        return createHook(HookTypeEnum.SUMMARIZATION, dto);
    }

    public HumanInTheLoopHook creatHumanInTheLoopHook(HumanInTheLoopHookDTO dto) {
        return createHook(HookTypeEnum.HUMAN_IN_THE_LOOP, dto);
    }

    public ModelCallLimitHook creatModelCallLimitHook(ModelCallLimitHookDTO dto) {
        return createHook(HookTypeEnum.MODEL_CALL_LIMIT, dto);
    }

    public PIIDetectionHook createPIIDetectionHook(PIIDetectionHookDTO dto) {
        return createHook(HookTypeEnum.PII_DETECTION, dto);
    }

    public SkillsAgentHook createSkillsAgentHook(SkillsAgentHookDTO dto) {
        return createHook(HookTypeEnum.SKILLS, dto);
    }

    @Override
    public void afterPropertiesSet() {
        applicationContext.getBeansOfType(HookCreator.class).values()
                .forEach(creator -> creatorMap.put(creator.getHookType(), creator));
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
