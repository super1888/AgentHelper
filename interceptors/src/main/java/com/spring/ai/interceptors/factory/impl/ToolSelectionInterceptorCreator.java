package com.spring.ai.interceptors.factory.impl;

import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.spring.ai.common.enums.InterceptorTypeEnum;
import com.spring.ai.interceptors.domain.dto.ToolSelectionInterceptorDTO;
import com.spring.ai.interceptors.factory.InterceptorCreator;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * ToolSelectionInterceptor 创建器。
 */
@Component
public class ToolSelectionInterceptorCreator implements InterceptorCreator {

    @Override
    public InterceptorTypeEnum getInterceptorType() {
        return InterceptorTypeEnum.TOOL_SELECTION;
    }

    @Override
    public Object create(Object dto) {
        ToolSelectionInterceptorDTO interceptorDTO = (ToolSelectionInterceptorDTO) dto;
        if (interceptorDTO == null) {
            throw new IllegalArgumentException("工具选择拦截器配置不能为空");
        }
        ToolSelectionInterceptor.Builder builder = new ToolSelectionInterceptor.Builder();
        builder.selectionModel(interceptorDTO.getSelectionModel());
        if (interceptorDTO.getSystemPrompt() != null) {
            builder.systemPrompt(interceptorDTO.getSystemPrompt());
        }
        if (interceptorDTO.getMaxTools() != null) {
            builder.maxTools(interceptorDTO.getMaxTools());
        }
        Set<String> alwaysInclude = interceptorDTO.getAlwaysInclude();
        if (alwaysInclude != null && !alwaysInclude.isEmpty()) {
            builder.alwaysInclude(alwaysInclude);
        }
        return builder.build();
    }
}
