package com.spring.ai.core.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 模型配置详情响应。
 */
@Value
@Builder
public class ModelDefinitionResponse {

    /**
     * 模型配置唯一编码。
     */
    String modelCode;

    /**
     * 模型名称。
     */
    String modelName;

    /**
     * 关联的提供商配置编码。
     */
    String providerConfigCode;

    /**
     * 提供商枚举值。
     */
    String providerEnum;

    /**
     * 提供商配置名称。
     */
    String providerName;

    /**
     * 模型类型。
     */
    String modelType;

    /**
     * 实际调用的模型标识。
     */
    String modelIdentifier;

    /**
     * 温度参数。
     */
    Double temperature;

    /**
     * Top-P 采样参数。
     */
    Double topP;

    /**
     * 存在惩罚参数。
     */
    Double presencePenalty;

    /**
     * 频率惩罚参数。
     */
    Double frequencyPenalty;

    /**
     * 最大输出 token 数。
     */
    Integer maxTokens;

    /**
     * 上下文窗口大小。
     */
    Integer contextWindow;

    /**
     * 每分钟请求数限制。
     */
    Integer rpmLimit;

    /**
     * 每分钟 token 数限制。
     */
    Integer tpmLimit;

    /**
     * 单次调用超时时间，单位毫秒。
     */
    Integer timeoutMs;

    /**
     * 是否支持流式输出。
     */
    Boolean supportStreaming;

    /**
     * 是否支持工具调用。
     */
    Boolean supportTools;

    /**
     * 是否支持视觉能力。
     */
    Boolean supportVision;

    /**
     * 是否支持 JSON Schema 输出。
     */
    Boolean supportJsonSchema;

    /**
     * 是否为默认模型。
     */
    Boolean defaultModel;

    /**
     * 配置状态。
     */
    String status;

    /**
     * 高级配置 JSON。
     */
    String advancedConfigJson;

    /**
     * 备注说明。
     */
    String remark;

    /**
     * 最近更新时间，毫秒时间戳。
     */
    Long updateTime;
}
