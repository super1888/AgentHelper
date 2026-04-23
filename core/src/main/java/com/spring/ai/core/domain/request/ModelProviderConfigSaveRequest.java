package com.spring.ai.core.domain.request;

import lombok.Data;

@Data
public class ModelProviderConfigSaveRequest {

    private String providerEnum;

    private String providerName;

    private String baseUrl;

    private String apiKey;

    private String organizationId;

    private String defaultHeadersJson;

    private String remark;

    private String status;
}
