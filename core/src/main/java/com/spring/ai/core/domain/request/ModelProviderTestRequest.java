package com.spring.ai.core.domain.request;

import lombok.Data;

/**
 * 模型提供商连通性测试请求。
 * 支持基于已有配置测试，也支持直接携带临时参数进行测试。
 */
@Data
public class ModelProviderTestRequest {

    /**
     * 已保存的提供商配置编码。
     */
    private String providerConfigCode;

    /**
     * 提供商枚举值。
     */
    private String providerEnum;

    /**
     * 自定义服务地址。
     */
    private String baseUrl;

    /**
     * 本次测试使用的明文 API Key。
     */
    private String apiKey;

    /**
     * 测试时使用的模型标识；为空时由系统按提供商补默认值。
     */
    private String testModelIdentifier;

    /**
     * 自定义测试提示词。
     */
    private String testPrompt;
}
