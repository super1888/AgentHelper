package com.spring.ai.core.domain.request;

import lombok.Data;

/**
 * 模型配置新增或编辑请求。
 */
@Data
public class ModelDefinitionSaveRequest {

    /**
     * 模型配置名称，用于前端展示与业务识别。
     */
    private String modelName;

    /**
     * 关联的模型提供商配置编码。
     */
    private String providerConfigCode;

    /**
     * 模型类型，默认使用 CHAT。
     */
    private String modelType;

    /**
     * 供应商侧的真实模型标识，例如 gpt-4.1。
     */
    private String modelIdentifier;

    /**
     * 温度参数。
     */
    private Double temperature;

    /**
     * Top-P 采样参数。
     */
    private Double topP;

    /**
     * 存在惩罚参数。
     */
    private Double presencePenalty;

    /**
     * 频率惩罚参数。
     */
    private Double frequencyPenalty;

    /**
     * 最大输出 token 数。
     */
    private Integer maxTokens;

    /**
     * 上下文窗口大小。
     */
    private Integer contextWindow;

    /**
     * 每分钟请求数限制。
     */
    private Integer rpmLimit;

    /**
     * 每分钟 token 数限制。
     */
    private Integer tpmLimit;

    /**
     * 单次调用超时时间，单位毫秒。
     */
    private Integer timeoutMs;

    /**
     * 是否支持流式输出。
     */
    private Boolean supportStreaming;

    /**
     * 是否支持工具调用。
     */
    private Boolean supportTools;

    /**
     * 是否支持视觉能力。
     */
    private Boolean supportVision;

    /**
     * 是否支持 JSON Schema 输出。
     */
    private Boolean supportJsonSchema;

    /**
     * 是否设为默认模型。
     */
    private Boolean defaultModel;

    /**
     * 扩展高级配置，要求为 JSON 字符串。
     */
    private String advancedConfigJson;

    /**
     * 备注说明。
     */
    private String remark;

    /**
     * 配置状态，例如 ENABLED 或 DISABLED。
     */
    private String status;
}
