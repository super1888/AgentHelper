package com.spring.ai.core.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModelProviderConfigResponse {

    String providerConfigCode;

    String providerEnum;

    String providerName;

    String baseUrl;

    String organizationId;

    String defaultHeadersJson;

    String status;

    String apiKeyMasked;

    Boolean apiKeyConfigured;

    String ownerUserName;

    Long updateTime;

    String remark;
}
