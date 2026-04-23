package com.spring.ai.core.facotry;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions.DashScopeChatOptionsBuilder;
import com.spring.ai.common.enums.ModelProviderEnum;
import com.spring.ai.core.domain.dto.ChatModelRequest;
import com.spring.ai.core.domain.dto.ChatOptionsDTO;
import com.spring.ai.core.modelApi.ScopeApi;
import jakarta.annotation.Resource;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
public class DynamicChatModelFactory {

    @Resource
    private ScopeApi scopeApi;

    public ChatModel create(ChatModelRequest request) {
        Assert.notNull(request, "ChatModelRequest must not be null");
        ModelProviderEnum provider = ModelProviderEnum.fromValue(request.getProvider());
        ChatOptionsDTO options = mergeOptions(request);

        return switch (provider) {
            case DASHSCOPE -> DashScopeChatModel.builder()
                    .dashScopeApi(scopeApi.getDashScopeApi(request.getApiKey(), request.getBaseUrl()))
                    .defaultOptions(buildDashScopeOptions(options))
                    .build();
            case DEEPSEEK -> DeepSeekChatModel.builder()
                    .deepSeekApi(scopeApi.getDeepSeekApi(request.getApiKey(), request.getBaseUrl()))
                    .defaultOptions(buildDeepSeekOptions(options))
                    .build();
            case OPENAI -> OpenAiChatModel.builder()
                    .openAiApi(scopeApi.getOpenAiApi(request.getApiKey(), request.getBaseUrl()))
                    .defaultOptions(buildOpenAiOptions(options))
                    .build();
            case ZHIPU -> new ZhiPuAiChatModel(
                    scopeApi.getZhiPuAiApi(request.getApiKey(), request.getBaseUrl()),
                    buildZhiPuAiOptions(options));
            case ANTHROPIC -> AnthropicChatModel.builder()
                    .anthropicApi(scopeApi.getAnthropicApi(request.getApiKey(), request.getBaseUrl()))
                    .defaultOptions(buildAnthropicOptions(options))
                    .build();
        };
    }

    public ChatClient createChatClient(ChatModelRequest request) {
        return ChatClient.create(create(request));
    }

    /**
     * 创建指定的大模型
     * @param model
     * @return
     */
    public ChatModel createDashScopeChatModel(String model) {
        ChatModelRequest request = new ChatModelRequest();
        request.setProvider(ModelProviderEnum.DASHSCOPE.name());
        request.setModel(model);
        return create(request);
    }

    private ChatOptionsDTO mergeOptions(ChatModelRequest request) {
        ChatOptionsDTO options = request.getOptions() == null ? new ChatOptionsDTO() : request.getOptions();
        if (StringUtils.hasText(request.getModel()) && !StringUtils.hasText(options.getModel())) {
            options.setModel(request.getModel().trim());
        }
        return options;
    }

    private DashScopeChatOptions buildDashScopeOptions(ChatOptionsDTO options) {
        DashScopeChatOptionsBuilder builder = DashScopeChatOptions.builder();
        if (StringUtils.hasText(options.getModel())) {
            builder.withModel(options.getModel().trim());
        }
        if (options.getTemperature() != null) {
            builder.withTemperature(options.getTemperature());
        }
        if (options.getMaxTokens() != null) {
            builder.withMaxToken(options.getMaxTokens());
        }
        if (options.getTopP() != null) {
            builder.withTopP(options.getTopP());
        }
        if (options.getTopK() != null) {
            builder.withTopK(options.getTopK());
        }
        return builder.build();
    }

    private DeepSeekChatOptions buildDeepSeekOptions(ChatOptionsDTO options) {
        DeepSeekChatOptions.Builder builder = DeepSeekChatOptions.builder();
        if (StringUtils.hasText(options.getModel())) {
            builder.model(options.getModel().trim());
        }
        if (options.getTemperature() != null) {
            builder.temperature(options.getTemperature());
        }
        if (options.getMaxTokens() != null) {
            builder.maxTokens(options.getMaxTokens());
        }
        if (options.getTopP() != null) {
            builder.topP(options.getTopP());
        }
        if (options.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(options.getFrequencyPenalty());
        }
        if (options.getPresencePenalty() != null) {
            builder.presencePenalty(options.getPresencePenalty());
        }
        if (options.getStopSequences() != null && !options.getStopSequences().isEmpty()) {
            builder.stop(options.getStopSequences());
        }
        return builder.build();
    }

    private OpenAiChatOptions buildOpenAiOptions(ChatOptionsDTO options) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        if (StringUtils.hasText(options.getModel())) {
            builder.model(options.getModel().trim());
        }
        if (options.getTemperature() != null) {
            builder.temperature(options.getTemperature());
        }
        if (options.getMaxTokens() != null) {
            builder.maxTokens(options.getMaxTokens());
        }
        if (options.getTopP() != null) {
            builder.topP(options.getTopP());
        }
        if (options.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(options.getFrequencyPenalty());
        }
        if (options.getPresencePenalty() != null) {
            builder.presencePenalty(options.getPresencePenalty());
        }
        if (options.getStopSequences() != null && !options.getStopSequences().isEmpty()) {
            builder.stop(options.getStopSequences());
        }
        return builder.build();
    }

    private ZhiPuAiChatOptions buildZhiPuAiOptions(ChatOptionsDTO options) {
        ZhiPuAiChatOptions.Builder builder = ZhiPuAiChatOptions.builder();
        if (StringUtils.hasText(options.getModel())) {
            builder.model(options.getModel().trim());
        }
        if (options.getTemperature() != null) {
            builder.temperature(options.getTemperature());
        }
        if (options.getMaxTokens() != null) {
            builder.maxTokens(options.getMaxTokens());
        }
        if (options.getTopP() != null) {
            builder.topP(options.getTopP());
        }
        if (options.getStopSequences() != null && !options.getStopSequences().isEmpty()) {
            builder.stop(options.getStopSequences());
        }
        return builder.build();
    }

    private AnthropicChatOptions buildAnthropicOptions(ChatOptionsDTO options) {
        AnthropicChatOptions.Builder builder = AnthropicChatOptions.builder();
        if (StringUtils.hasText(options.getModel())) {
            builder.model(options.getModel().trim());
        }
        if (options.getTemperature() != null) {
            builder.temperature(options.getTemperature());
        }
        if (options.getMaxTokens() != null) {
            builder.maxTokens(options.getMaxTokens());
        }
        if (options.getTopP() != null) {
            builder.topP(options.getTopP());
        }
        if (options.getTopK() != null) {
            builder.topK(options.getTopK());
        }
        if (options.getStopSequences() != null && !options.getStopSequences().isEmpty()) {
            builder.stopSequences(options.getStopSequences());
        }
        return builder.build();
    }
}
