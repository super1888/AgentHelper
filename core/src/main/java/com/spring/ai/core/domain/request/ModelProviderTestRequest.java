package com.spring.ai.core.domain.request;

import lombok.Data;

@Data
public class ModelProviderTestRequest {

    private String providerConfigCode;

    private String providerEnum;

    private String baseUrl;

    private String apiKey;

    private String testModelIdentifier;

    private String testPrompt;
}
