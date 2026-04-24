package com.spring.ai.core.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 模型提供商配置响应。
 */
@Value
@Builder
public class ModelProviderConfigResponse {

    /**
     * 提供商配置编码。
     */
    String providerConfigCode;

    /**
     * 提供商枚举值。
     */
    String providerEnum;

    /**
     * 提供商配置名称。
     */
    String providerName;

    /**
     * 自定义服务地址。
     */
    String baseUrl;

    /**
     * 组织标识。
     */
    String organizationId;

    /**
     * 默认请求头 JSON 配置。
     */
    String defaultHeadersJson;

    /**
     * 配置状态。
     */
    String status;

    /**
     * 脱敏后的 API Key。
     */
    String apiKeyMasked;

    /**
     * 是否已配置 API Key。
     */
    Boolean apiKeyConfigured;

    /**
     * 配置归属人名称。
     */
    String ownerUserName;

    /**
     * 最近更新时间，毫秒时间戳。
     */
    Long updateTime;

    /**
     * 备注说明。
     */
    String remark;
}
