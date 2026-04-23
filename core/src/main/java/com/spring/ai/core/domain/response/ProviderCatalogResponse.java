package com.spring.ai.core.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProviderCatalogResponse {

    String providerEnum;

    String providerLabel;
}
