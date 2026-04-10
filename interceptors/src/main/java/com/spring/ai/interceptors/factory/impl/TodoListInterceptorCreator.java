package com.spring.ai.interceptors.factory.impl;

import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import com.spring.ai.common.enums.InterceptorTypeEnum;
import com.spring.ai.interceptors.domain.dto.TodoListInterceptorDTO;
import com.spring.ai.interceptors.factory.InterceptorCreator;
import org.springframework.stereotype.Component;

/**
 * TodoListInterceptor 创建器。
 */
@Component
public class TodoListInterceptorCreator implements InterceptorCreator {

    @Override
    public InterceptorTypeEnum getInterceptorType() {
        return InterceptorTypeEnum.TODO_LIST;
    }

    @Override
    public Object create(Object dto) {
        TodoListInterceptorDTO interceptorDTO = (TodoListInterceptorDTO) dto;
        if (interceptorDTO == null) {
            throw new IllegalArgumentException("TodoListInterceptorDTO 不能为空");
        }
        return new TodoListInterceptor.Builder()
                .systemPrompt(interceptorDTO.getSystemPrompt())
                .toolDescription(interceptorDTO.getToolDescription())
                .build();
    }
}
