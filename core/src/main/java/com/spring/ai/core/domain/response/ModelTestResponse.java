package com.spring.ai.core.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 模型调用测试响应。
 */
@Value
@Builder
public class ModelTestResponse {

    /**
     * 调用是否成功。
     */
    Boolean success;

    /**
     * 实际使用的提供商。
     */
    String providerEnum;

    /**
     * 实际调用的模型标识。
     */
    String modelIdentifier;

    /**
     * 模型返回的文本内容。
     */
    String responseContent;

    /**
     * 调用耗时，单位毫秒。
     */
    Long elapsedMs;
}
