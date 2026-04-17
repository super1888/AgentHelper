package com.spring.ai.core.modelApi;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    public DashScopeApi getDashScopeApi(String apiKey) {
        return DashScopeApi.builder()
                .apiKey(resolveApiKey(apiKey, dashscopeApiKey, "dashscope"))
                .build();
    }

    public DeepSeekApi getDeepSeekApi(String apiKey) {
        return DeepSeekApi.builder()
                .apiKey(resolveApiKey(apiKey, deepseekApiKey, "deepseek"))
                .build();
    }

    public AnthropicApi getAnthropicApi(String apiKey) {
        return AnthropicApi.builder()
                .apiKey(resolveApiKey(apiKey, anthropicApiKey, "anthropic"))
                .build();
    }

    public OpenAiApi getOpenAiApi(String apiKey) {
        return OpenAiApi.builder()
                .apiKey(resolveApiKey(apiKey, openAiApiKey, "openai"))
                .build();
    }

    public ZhiPuAiApi getZhiPuAiApi(String apiKey) {
        return ZhiPuAiApi.builder()
                .apiKey(resolveApiKey(apiKey, zhiPuAiApiKey, "zhipuai"))
                .build();
    }

    private String resolveApiKey(String requestApiKey, String configuredApiKey, String provider) {
        String resolved = StringUtils.isBlank(requestApiKey) ? configuredApiKey : requestApiKey;
        if (StringUtils.isBlank(resolved)) {
            throw new IllegalArgumentException("未配置模型提供商访问密钥：" + provider);
        }
        return resolved;
    }
}
