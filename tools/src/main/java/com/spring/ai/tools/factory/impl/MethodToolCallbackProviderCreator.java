package com.spring.ai.tools.factory.impl;

import com.spring.ai.common.enums.ToolFactoryTypeEnum;
import com.spring.ai.tools.domain.dto.MethodToolCallbackProviderDTO;
import com.spring.ai.tools.factory.ToolCreator;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * MethodToolCallbackProvider 创建器。
 */
@Component
public class MethodToolCallbackProviderCreator implements ToolCreator {

    @Override
    public ToolFactoryTypeEnum getToolFactoryType() {
        return ToolFactoryTypeEnum.METHOD_TOOL_CALLBACK_PROVIDER;
    }

    @Override
    public Object create(Object dto) {
        MethodToolCallbackProviderDTO providerDTO = (MethodToolCallbackProviderDTO) dto;
        if (providerDTO == null) {
            throw new IllegalArgumentException("方法工具回调提供器配置不能为空");
        }
        return MethodToolCallbackProvider.builder()
                .toolObjects(providerDTO.getTools())
                .build()
                .getToolCallbacks();
    }
}
