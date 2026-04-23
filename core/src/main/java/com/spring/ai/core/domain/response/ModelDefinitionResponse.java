package com.spring.ai.core.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModelDefinitionResponse {

    String modelCode;

    String modelName;

    String providerConfigCode;

    String providerEnum;

    String providerName;

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

    String status;

    String advancedConfigJson;

    String remark;

    Long updateTime;
}
