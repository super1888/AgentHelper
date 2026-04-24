package com.spring.ai.core.domain.request;

import lombok.Data;

/**
 * 模型连接配置测试请求。
 */
@Data
public class ModelConnectionTestRequest {

    /**
     * 已存在模型配置编码；编辑态测试时用于补齐旧密钥等信息。
     */
    private String modelCode;

    private String providerEnum;

    private String baseUrl;

    private String apiKey;

    private String modelIdentifier;

    private Double temperature;

    private Double topP;

    private Double presencePenalty;

    private Double frequencyPenalty;

    private Integer maxTokens;

    private String testPrompt;
}
