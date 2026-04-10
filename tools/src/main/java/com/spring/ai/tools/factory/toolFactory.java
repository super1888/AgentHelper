package com.spring.ai.tools.factory;

import com.spring.ai.common.enums.ToolFactoryTypeEnum;
import com.spring.ai.tools.domain.dto.MethodToolCallbackProviderDTO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Tool 工厂分发器。
 */
@Component
public class ToolFactory implements InitializingBean, ApplicationContextAware {

    private final Map<ToolFactoryTypeEnum, ToolCreator> creatorMap = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;

    @SuppressWarnings("unchecked")
    public <T> T createToolComponent(ToolFactoryTypeEnum type, Object dto) {
        ToolCreator creator = creatorMap.get(type);
        if (creator == null) {
            throw new IllegalArgumentException("不支持的 Tool 工厂类型: " + type);
        }
        return (T) creator.create(dto);
    }

    public ToolCallback[] creatMethodToolCallbackProvider(MethodToolCallbackProviderDTO dto) {
        return createToolComponent(ToolFactoryTypeEnum.METHOD_TOOL_CALLBACK_PROVIDER, dto);
    }

    @Override
    public void afterPropertiesSet() {
        applicationContext.getBeansOfType(ToolCreator.class).values()
                .forEach(creator -> creatorMap.put(creator.getToolFactoryType(), creator));
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
