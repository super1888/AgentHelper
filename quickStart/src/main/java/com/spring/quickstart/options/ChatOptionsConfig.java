package com.spring.quickstart.options;

import com.spring.ai.core.domain.dto.ChatOptionsDTO;
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
            throw new IllegalArgumentException("模型不能为空");
        }

        if (dto.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(dto.getFrequencyPenalty());
        } else {
            throw new IllegalArgumentException("频率惩罚参数不能为空");
        }

        if (dto.getMaxTokens() != null) {
            builder.maxTokens(dto.getMaxTokens());
        } else {
            throw new IllegalArgumentException("最大令牌数不能为空");
        }

        if (dto.getPresencePenalty() != null) {
            builder.presencePenalty(dto.getPresencePenalty());
        } else {
            throw new IllegalArgumentException("存在惩罚参数不能为空");
        }

        if (dto.getStopSequences() != null) {
            builder.stopSequences(dto.getStopSequences());
        } else {
            throw new IllegalArgumentException("停止序列不能为空");
        }

        if (dto.getTemperature() != null) {
            builder.temperature(dto.getTemperature());
        } else {
            throw new IllegalArgumentException("温度参数不能为空");
        }

        if (dto.getTopK() != null) {
            builder.topK(dto.getTopK());
        } else {
            throw new IllegalArgumentException("候选数量不能为空");
        }

        if (dto.getTopP() != null) {
            builder.topP(dto.getTopP());
        } else {
            throw new IllegalArgumentException("概率阈值不能为空");
        }

        return builder.build();
    }

}
