package com.spring.ai.common.enums;

/**
 * 支持的厂商
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/8
 */
public enum ModelProviderEnum {
    OPENAI,
    DEEPSEEK,
    ZHIPU,
    ANTHROPIC,
    DASHSCOPE;

    public static ModelProviderEnum fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Model provider must not be blank");
        }
        String normalized = value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase();
        if ("ZHIPUAI".equals(normalized)) {
            return ZHIPU;
        }
        return ModelProviderEnum.valueOf(normalized);
    }
}
