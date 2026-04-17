package com.spring.ai.interceptors.factory.impl;

import com.alibaba.cloud.ai.graph.agent.interceptor.toolretry.ToolRetryInterceptor;
import com.spring.ai.common.enums.InterceptorTypeEnum;
import com.spring.ai.interceptors.domain.dto.ToolRetryInterceptorDTO;
import com.spring.ai.interceptors.factory.InterceptorCreator;
import org.springframework.stereotype.Component;

/**
 * ToolRetryInterceptor 创建器。
 */
@Component
public class ToolRetryInterceptorCreator implements InterceptorCreator {

    @Override
    public InterceptorTypeEnum getInterceptorType() {
        return InterceptorTypeEnum.TOOL_RETRY;
    }

    @Override
    public Object create(Object dto) {
        ToolRetryInterceptorDTO interceptorDTO = (ToolRetryInterceptorDTO) dto;
        if (interceptorDTO == null) {
            throw new IllegalArgumentException("工具重试拦截器配置不能为空");
        }
        return ToolRetryInterceptor.builder()
                .maxRetries(interceptorDTO.getMaxRetries())
                .toolNames(interceptorDTO.getToolNames())
                .retryOn(interceptorDTO.getRetryOn())
                .onFailure(interceptorDTO.getOnFailure())
                .errorFormatter(interceptorDTO.getErrorFormatter())
                .backoffFactor(interceptorDTO.getBackoffFactor())
                .initialDelay(interceptorDTO.getInitialDelayMs())
                .maxDelay(interceptorDTO.getMaxDelayMs())
                .jitter(interceptorDTO.isJitter())
                .build();
    }
}
