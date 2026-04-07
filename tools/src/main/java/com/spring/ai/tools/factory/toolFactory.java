package com.spring.ai.tools.factory;

import com.spring.ai.tools.domain.dto.MethodToolCallbackProviderDTO;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/7
 */
public class toolFactory {

    /**
     * 核心能力是扫描并自动将带有 @Tool 注解的方法转换为 ToolCallback 实例
     *
     * @param dto
     * @return
     */
    public ToolCallback[] creatMethodToolCallbackProvider(MethodToolCallbackProviderDTO dto) {

        return MethodToolCallbackProvider.builder().toolObjects(dto.getTools()).build().getToolCallbacks();

    }

}
