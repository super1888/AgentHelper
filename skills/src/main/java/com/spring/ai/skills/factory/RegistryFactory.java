package com.spring.ai.skills.factory;

import com.spring.ai.common.enums.SkillFactoryTypeEnum;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Skills 注册工厂分发器。
 */
@Component
public class RegistryFactory implements InitializingBean, ApplicationContextAware {

    private final Map<SkillFactoryTypeEnum, SkillCreator> creatorMap = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;

    @SuppressWarnings("unchecked")
    public <T> T createSkillComponent(SkillFactoryTypeEnum type, Object dto) {
        SkillCreator creator = creatorMap.get(type);
        if (creator == null) {
            throw new IllegalArgumentException("不支持的技能工厂类型：" + type);
        }
        return (T) creator.create(dto);
    }

    @Override
    public void afterPropertiesSet() {
        applicationContext.getBeansOfType(SkillCreator.class).values()
                .forEach(creator -> creatorMap.put(creator.getSkillFactoryType(), creator));
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
