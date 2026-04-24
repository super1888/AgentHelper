package com.spring.ai.core.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 模型连接配置响应。
 */
@Value
@Builder
public class ModelConnectionResponse {

    String modelCode;

    String providerConfigCode;

    String connectionName;

    String providerEnum;

    String baseUrl;

    String organizationId;

    String defaultHeadersJson;

    String apiKeyMasked;

    Boolean apiKeyConfigured;

    String modelType;

    String modelIdentifier;

    Double temperature;

    Double topP;

    Double presencePenalty;

    Double frequencyPenalty;

    Integer maxTokens;

    Integer contextWindow;

    Integer rpmLimit;

    Integer tpmLimit;

    Integer timeoutMs;

    Boolean supportStreaming;

    Boolean supportTools;

    Boolean supportVision;

    Boolean supportJsonSchema;

    Boolean defaultModel;

    String advancedConfigJson;

    String status;

    String remark;

    Long updateTime;
}
