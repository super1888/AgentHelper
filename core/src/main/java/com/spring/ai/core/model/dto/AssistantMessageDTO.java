package com.spring.ai.core.model.dto;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/29
 */

import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;

import org.springframework.ai.chat.messages.AssistantMessage.Builder;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.content.Media;

@Data
public class AssistantMessageDTO {

    /**
     * 用户输入文本内容
     */
    private String text;

    /**
     * metadata消息的元数据映射
     */
    private Map<String, Object> metadata;
    /**
     * 模型进行的工具调用列表
     */
    private List<ToolCall> toolCalls;

    /**
     * 用户输入的媒体内容
     */
    private List<Media> media;


    public static AssistantMessage getAssistantMessage(String textContent, Map<String, Object> metadata, List<ToolCall> toolCalls,
            List<Media> media) {
        Builder builder = AssistantMessage.builder();

        if (textContent != null) {
            builder.content(textContent);
        }
        if (metadata != null) {
            builder.properties(metadata);
        }

        if (toolCalls != null) {
            builder.toolCalls(toolCalls);
        }

        if (media != null) {
            builder.media(media);
        }

        return builder.build();
    }

}




