package com.spring.ai.interceptors.factory.impl;

import com.alibaba.cloud.ai.graph.agent.interceptor.toolemulator.ToolEmulatorInterceptor;
import com.spring.ai.common.enums.InterceptorTypeEnum;
import com.spring.ai.interceptors.domain.dto.ToolEmulatorInterceptorDTO;
import com.spring.ai.interceptors.factory.InterceptorCreator;
import org.springframework.stereotype.Component;

/**
 * ToolEmulatorInterceptor 创建器。
 */
@Component
public class ToolEmulatorInterceptorCreator implements InterceptorCreator {

    @Override
    public InterceptorTypeEnum getInterceptorType() {
        return InterceptorTypeEnum.TOOL_EMULATOR;
    }

    @Override
    public Object create(Object dto) {
        ToolEmulatorInterceptorDTO interceptorDTO = (ToolEmulatorInterceptorDTO) dto;
        if (interceptorDTO == null) {
            throw new IllegalArgumentException("工具模拟拦截器配置不能为空");
        }

        ToolEmulatorInterceptor.Builder builder = new ToolEmulatorInterceptor.Builder();
        if (interceptorDTO.getEmulatorModel() == null) {
            throw new IllegalStateException("模拟模型配置不能为空");
        }
        builder.model(interceptorDTO.getEmulatorModel());
        if (interceptorDTO.getToolsToEmulate() != null && !interceptorDTO.getToolsToEmulate().isEmpty()) {
            builder.addTools(interceptorDTO.getToolsToEmulate());
        }
        builder.emulateAllTools(interceptorDTO.isEmulateAll());
        if (interceptorDTO.getPromptTemplate() != null && !interceptorDTO.getPromptTemplate().isBlank()) {
            builder.promptTemplate(interceptorDTO.getPromptTemplate());
        }
        return builder.build();
    }
}
