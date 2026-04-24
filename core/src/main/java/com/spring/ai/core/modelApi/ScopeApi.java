package com.spring.ai.core.modelApi;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import io.micrometer.common.util.StringUtils;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 各供应商底层 API 客户端创建器。 优先使用调用方显式传入的密钥与地址；若未传入，则回退到系统配置。
 */
@Component
public class ScopeApi {

    @Value("${spring.ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${spring.ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${spring.ai.anthropic.api-key:}")
    private String anthropicApiKey;

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${spring.ai.zhipuai.api-key:}")
    private String zhiPuAiApiKey;

    /**
     * 创建 DashScope API 客户端，并允许覆盖默认服务地址。
     */
    public DashScopeApi getDashScopeApi(String apiKey, String baseUrl) {
        DashScopeApi.Builder builder = DashScopeApi.builder()
                .apiKey(resolveApiKey(apiKey, dashscopeApiKey, "dashscope"));
        if (StringUtils.isNotBlank(baseUrl)) {
            builder.baseUrl(baseUrl.trim());
        }
        return builder.build();
    }


    /**
     * 创建 DeepSeek API 客户端，并允许覆盖默认服务地址。
     */
    public DeepSeekApi getDeepSeekApi(String apiKey, String baseUrl) {
        DeepSeekApi.Builder builder = DeepSeekApi.builder()
                .apiKey(resolveApiKey(apiKey, deepseekApiKey, "deepseek"));
        if (StringUtils.isNotBlank(baseUrl)) {
            builder.baseUrl(baseUrl.trim());
        }
        return builder.build();
    }

    /**
     * 创建 Anthropic API 客户端，并允许覆盖默认服务地址。
     */
    public AnthropicApi getAnthropicApi(String apiKey, String baseUrl) {
        AnthropicApi.Builder builder = AnthropicApi.builder()
                .apiKey(resolveApiKey(apiKey, anthropicApiKey, "anthropic"));
        if (StringUtils.isNotBlank(baseUrl)) {
            builder.baseUrl(baseUrl.trim());
        }
        return builder.build();
    }

    /**
     * 创建 OpenAI API 客户端，并允许覆盖默认服务地址。
     */
    public OpenAiApi getOpenAiApi(String apiKey, String baseUrl) {
        OpenAiApi.Builder builder = OpenAiApi.builder()
                .apiKey(resolveApiKey(apiKey, openAiApiKey, "openai"));
        if (StringUtils.isNotBlank(baseUrl)) {
            builder.baseUrl(baseUrl.trim());
        }
        return builder.build();
    }

    /**
     * 创建智谱 API 客户端，地址使用框架默认值。
     */
    public ZhiPuAiApi getZhiPuAiApi(String apiKey) {
        return ZhiPuAiApi.builder()
                .apiKey(resolveApiKey(apiKey, zhiPuAiApiKey, "zhipuai"))
                .build();
    }

    /**
     * 创建智谱 API 客户端，并允许覆盖默认服务地址。
     */
    public ZhiPuAiApi getZhiPuAiApi(String apiKey, String baseUrl) {
        ZhiPuAiApi.Builder builder = ZhiPuAiApi.builder()
                .apiKey(resolveApiKey(apiKey, zhiPuAiApiKey, "zhipuai"));
        if (StringUtils.isNotBlank(baseUrl)) {
            builder.baseUrl(baseUrl.trim());
        }
        return builder.build();
    }

    /**
     * 解析最终使用的 API Key，优先级为请求参数高于系统配置。
     */
    private String resolveApiKey(String requestApiKey, String configuredApiKey, String provider) {
        String resolved = StringUtils.isBlank(requestApiKey) ? configuredApiKey : requestApiKey;
        if (StringUtils.isBlank(resolved)) {
            throw new IllegalArgumentException("没有找到配置的api-key" + provider);
        }
        return resolved;
    }
}
