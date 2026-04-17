package com.spring.ai.interceptors.factory.impl;

import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.spring.ai.common.enums.InterceptorTypeEnum;
import com.spring.ai.interceptors.domain.dto.ContextEditingInterceptorDTO;
import com.spring.ai.interceptors.factory.InterceptorCreator;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * ContextEditingInterceptor 创建器。
 */
@Component
public class ContextEditingInterceptorCreator implements InterceptorCreator {

    @Override
    public InterceptorTypeEnum getInterceptorType() {
        return InterceptorTypeEnum.CONTEXT_EDITING;
    }

    @Override
    public Object create(Object dto) {
        ContextEditingInterceptorDTO interceptorDTO = (ContextEditingInterceptorDTO) dto;
        if (interceptorDTO == null) {
            throw new IllegalArgumentException("上下文编辑拦截器配置不能为空");
        }

        ContextEditingInterceptor.Builder builder = new ContextEditingInterceptor.Builder();
        builder.trigger(interceptorDTO.getTrigger());
        builder.clearAtLeast(interceptorDTO.getClearAtLeast());
        builder.keep(interceptorDTO.getKeep());
        builder.clearToolInputs(interceptorDTO.isClearToolInputs());
        builder.placeholder(interceptorDTO.getPlaceholder());
        builder.tokenCounter(interceptorDTO.getTokenCounter());

        Set<String> excludeTools = interceptorDTO.getExcludeTools();
        if (excludeTools != null && !excludeTools.isEmpty()) {
            builder.excludeTools(excludeTools);
        }
        return builder.build();
    }
}
