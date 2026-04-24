package com.spring.ai.core.domain.request;

import lombok.Data;

/**
 * 模型连接配置新增或编辑请求。
 */
@Data
public class ModelConnectionSaveRequest {

    /**
     * 已存在模型配置编码；为空时表示创建。
     */
    private String modelCode;

    /**
     * 连接配置名称，作为页面展示名称。
     */
    private String connectionName;

    /**
     * 提供商枚举。
     */
    private String providerEnum;

    /**
     * 自定义服务地址。
     */
    private String baseUrl;

    /**
     * 提供商访问密钥；编辑时为空则沿用原值。
     */
    private String apiKey;

    /**
     * 组织标识。
     */
    private String organizationId;

    /**
     * 默认请求头 JSON。
     */
    private String defaultHeadersJson;

    /**
     * 模型类型。
     */
    private String modelType;

    /**
     * 模型标识。
     */
    private String modelIdentifier;

    private Double temperature;

    private Double topP;

    private Double presencePenalty;

    private Double frequencyPenalty;

    private Integer maxTokens;

    private Integer contextWindow;

    private Integer rpmLimit;

    private Integer tpmLimit;

    private Integer timeoutMs;

    private Boolean supportStreaming;

    private Boolean supportTools;

    private Boolean supportVision;

    private Boolean supportJsonSchema;

    private Boolean defaultModel;

    private String advancedConfigJson;

    /**
     * 统一状态；保存时同步到 provider 与 model。
     */
    private String status;

    private String remark;
}
