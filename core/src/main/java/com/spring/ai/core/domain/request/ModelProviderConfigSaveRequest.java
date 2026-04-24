package com.spring.ai.core.domain.request;

import lombok.Data;

/**
 * 模型提供商配置新增或编辑请求。
 */
@Data
public class ModelProviderConfigSaveRequest {

    /**
     * 提供商枚举值，例如 OPENAI、DASHSCOPE。
     */
    private String providerEnum;

    /**
     * 当前租户下自定义的提供商配置名称。
     */
    private String providerName;

    /**
     * 自定义服务地址，兼容代理网关或私有中转地址。
     */
    private String baseUrl;

    /**
     * 提供商访问密钥。
     */
    private String apiKey;

    /**
     * 部分平台需要的组织标识。
     */
    private String organizationId;

    /**
     * 默认请求头扩展配置，要求为 JSON 字符串。
     */
    private String defaultHeadersJson;

    /**
     * 备注说明。
     */
    private String remark;

    /**
     * 配置状态。
     */
    private String status;
}
