package com.spring.ai.prompt.factory;

import com.spring.ai.common.enums.PromptTemplateTypeEnum;
import com.spring.ai.prompt.domain.dto.SystemPromptTemplateDTO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Prompt 模板工厂分发器。
 */
@Component
public class PromptTemplateFactory implements InitializingBean, ApplicationContextAware {

    private final Map<PromptTemplateTypeEnum, PromptTemplateCreator> creatorMap = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;

    /**
     * 按模板类型创建目标模板对象。
     */
    @SuppressWarnings("unchecked")
    public <T> T createPromptTemplate(PromptTemplateTypeEnum type, Object dto) {
        PromptTemplateCreator creator = creatorMap.get(type);
        if (creator == null) {
            throw new IllegalArgumentException("不支持的提示词模板类型：" + type);
        }
        return (T) creator.create(dto);
    }

    /**
     * 创建系统提示词模板。
     */
    public SystemPromptTemplate createSystemPromptTemplate(SystemPromptTemplateDTO dto) {
        return createPromptTemplate(PromptTemplateTypeEnum.SYSTEM_PROMPT_TEMPLATE, dto);
    }

    /**
     * 启动后收集全部模板创建器并建立映射。
     */
    @Override
    public void afterPropertiesSet() {
        applicationContext.getBeansOfType(PromptTemplateCreator.class).values()
                .forEach(creator -> creatorMap.put(creator.getPromptTemplateType(), creator));
    }

    /**
     * 注入 Spring 上下文供创建器扫描使用。
     */
    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
