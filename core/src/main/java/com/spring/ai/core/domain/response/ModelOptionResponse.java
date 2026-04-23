package com.spring.ai.core.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModelOptionResponse {

    String modelCode;

    String modelName;

    String providerConfigCode;

    String providerEnum;

    String providerName;

    String modelIdentifier;

    String modelType;

    Boolean defaultModel;
}
