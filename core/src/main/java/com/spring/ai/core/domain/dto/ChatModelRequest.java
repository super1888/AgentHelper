package com.spring.ai.core.domain.dto;

import lombok.Data;

/**
 * 动态创建模型时使用的请求对象。
 * 用于把提供商信息、鉴权信息和运行参数统一传递给模型工厂。
 */
@Data
public class ChatModelRequest {

    /**
     * 模型提供商枚举值。
     */
    private String provider;

    /**
     * 默认模型标识。
     */
    private String model;

    /**
     * 调用模型接口使用的明文密钥。
     */
    private String apiKey;

    /**
     * 模型服务自定义接入地址。
     */
    private String baseUrl;

    /**
     * 模型调用参数。
     */
    private ChatOptionsDTO options;
}
