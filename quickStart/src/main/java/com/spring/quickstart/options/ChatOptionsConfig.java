package com.spring.quickstart.options;

import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/29
 */
public class ChatOptionsConfig {

    public static ChatOptions buildChatOptions(ChatOptionsDTO dto) {
        ChatOptions.Builder builder = ChatOptions.builder();

        if (dto.getModel() != null) {
            builder.model(dto.getModel());
        } else {
            throw new IllegalArgumentException("Model cannot be null");
        }

        if (dto.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(dto.getFrequencyPenalty());
        } else {
            throw new IllegalArgumentException("Frequency Penalty cannot be null");
        }

        if (dto.getMaxTokens() != null) {
            builder.maxTokens(dto.getMaxTokens());
        } else {
            throw new IllegalArgumentException("Max Tokens cannot be null");
        }

        if (dto.getPresencePenalty() != null) {
            builder.presencePenalty(dto.getPresencePenalty());
        } else {
            throw new IllegalArgumentException("Presence Penalty cannot be null");
        }

        if (dto.getStopSequences() != null) {
            builder.stopSequences(dto.getStopSequences());
        } else {
            throw new IllegalArgumentException("Stop Sequences cannot be null");
        }

        if (dto.getTemperature() != null) {
            builder.temperature(dto.getTemperature());
        } else {
            throw new IllegalArgumentException("Temperature cannot be null");
        }

        if (dto.getTopK() != null) {
            builder.topK(dto.getTopK());
        } else {
            throw new IllegalArgumentException("TopK cannot be null");
        }

        if (dto.getTopP() != null) {
            builder.topP(dto.getTopP());
        } else {
            throw new IllegalArgumentException("TopP cannot be null");
        }

        return builder.build();
    }

}
