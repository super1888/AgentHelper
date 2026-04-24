package com.spring.ai.core.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 模型提供商目录响应。
 */
@Value
@Builder
public class ProviderCatalogResponse {

    /**
     * 提供商枚举值。
     */
    String providerEnum;

    /**
     * 提供商展示名称。
     */
    String providerLabel;
}
