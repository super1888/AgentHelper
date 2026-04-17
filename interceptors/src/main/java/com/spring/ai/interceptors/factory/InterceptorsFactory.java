package com.spring.ai.interceptors.factory;

import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolemulator.ToolEmulatorInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.spring.ai.common.enums.InterceptorTypeEnum;
import com.spring.ai.interceptors.domain.dto.ContextEditingInterceptorDTO;
import com.spring.ai.interceptors.domain.dto.TodoListInterceptorDTO;
import com.spring.ai.interceptors.domain.dto.ToolEmulatorInterceptorDTO;
import com.spring.ai.interceptors.domain.dto.ToolRetryInterceptorDTO;
import com.spring.ai.interceptors.domain.dto.ToolSelectionInterceptorDTO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * Interceptor 工厂分发器。
 */
@Service
public class InterceptorsFactory implements InitializingBean, ApplicationContextAware {

    private final Map<InterceptorTypeEnum, InterceptorCreator> creatorMap = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;

    @SuppressWarnings("unchecked")
    public <T> T createInterceptor(InterceptorTypeEnum type, Object dto) {
        InterceptorCreator creator = creatorMap.get(type);
        if (creator == null) {
            throw new IllegalArgumentException("不支持的拦截器类型：" + type);
        }
        return (T) creator.create(dto);
    }

    public ToolRetryInterceptor createToolRetryInterceptor(ToolRetryInterceptorDTO dto) {
        return createInterceptor(InterceptorTypeEnum.TOOL_RETRY, dto);
    }

    public TodoListInterceptor createTodoListInterceptor(TodoListInterceptorDTO dto) {
        return createInterceptor(InterceptorTypeEnum.TODO_LIST, dto);
    }

    public ToolSelectionInterceptor createToolSelectionInterceptor(ToolSelectionInterceptorDTO dto) {
        return createInterceptor(InterceptorTypeEnum.TOOL_SELECTION, dto);
    }

    public ToolEmulatorInterceptor createToolEmulatorInterceptor(ToolEmulatorInterceptorDTO dto) {
        return createInterceptor(InterceptorTypeEnum.TOOL_EMULATOR, dto);
    }

    public ContextEditingInterceptor createContextEditingInterceptor(ContextEditingInterceptorDTO dto) {
        return createInterceptor(InterceptorTypeEnum.CONTEXT_EDITING, dto);
    }

    @Override
    public void afterPropertiesSet() {
        applicationContext.getBeansOfType(InterceptorCreator.class).values()
                .forEach(creator -> creatorMap.put(creator.getInterceptorType(), creator));
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
