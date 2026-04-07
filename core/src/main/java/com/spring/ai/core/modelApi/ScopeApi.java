package com.spring.ai.core.modelApi;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/29
 */
@Component
public class ScopeApi {

    // 从配置文件中注入API密钥
    @Value("${spring.ai.dashscope.api-key}")  // 存储API密钥的私有变量
    private String dashscopeApiKey;

    @Value("${spring.ai.deepseek.api-key}")
    private String deepseekApiKey;
    @Value("${spring.ai.anthropic.api-key}")
    private String anthropicApi;
    @Value("${spring.ai.openai.api-key}")
    private String openAiApi;

    /**
     * 获取DashScopeApi实例的方法
     *
     * @param getApiKey 传入的API密钥，如果为空则使用配置文件中的默认值
     * @return DashScopeApi 返回配置好的DashScopeApi实例
     */

    // 检查传入的API密钥是否为空，如果为空则使用配置文件中的默认值
    public DashScopeApi getDashScopeApi(String getApiKey) {
        if (StringUtils.isBlank(getApiKey)) {
            getApiKey = dashscopeApiKey;
        }
        // 创建模型实例
        return DashScopeApi.builder()
                .apiKey(getApiKey)
                .build();
    }

    public DeepSeekApi getDeepSeekApi(String getApiKey) {

        if (StringUtils.isBlank(getApiKey)) {
            getApiKey = deepseekApiKey;
        }
        // 创建模型实例
        return DeepSeekApi.builder()
                .apiKey(getApiKey)
                .build();
    }

    public AnthropicApi getAnthropicApi(String getApiKey) {

        if (StringUtils.isBlank(getApiKey)) {
            getApiKey = anthropicApi;
        }
        // 创建模型实例
        return AnthropicApi.builder()
                .apiKey(getApiKey)
                .build();
    }

    public OpenAiApi getOpenAiApi(String getApiKey) {

        if (StringUtils.isBlank(getApiKey)) {
            getApiKey = openAiApi;
        }
        // 创建模型实例
        return OpenAiApi.builder()
                .apiKey(getApiKey)
                .build();
    }


}
