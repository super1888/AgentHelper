package com.spring.ai.core.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 模型选项响应。
 * 用于前端下拉选择等轻量级场景。
 */
@Value
@Builder
public class ModelOptionResponse {

    /**
     * 模型配置编码。
     */
    String modelCode;

    /**
     * 模型名称。
     */
    String modelName;

    /**
     * 提供商配置编码。
     */
    String providerConfigCode;

    /**
     * 提供商枚举值。
     */
    String providerEnum;

    /**
     * 提供商名称。
     */
    String providerName;

    /**
     * 模型标识。
     */
    String modelIdentifier;

    /**
     * 模型类型。
     */
    String modelType;

    /**
     * 是否为默认模型。
     */
    Boolean defaultModel;
}
