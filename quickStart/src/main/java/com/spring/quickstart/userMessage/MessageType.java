package com.spring.quickstart.userMessage;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * 消息类型
 * <p>
 * System Message（系统消息） - 告诉模型如何行为并为交互提供上下文 User Message（用户消息） - 表示用户输入和与模型的交互 Assistant Message（助手消息） - 模型生成的响应，包括文本内容、工具调用和元数据 Tool
 * ResponseMessage（工具响应消息） - 表示工具调用的输出
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/29
 */
@Data
public class MessageType {

    /**
     * （系统消息） - 告诉模型如何行为并为交互提供上下文
     */
    private SystemMessage systemMessage;

    /**
     * （用户消息） - 表示用户输入和与模型的交互 UserMessageDTO.getUserMessage()
     */
    private UserMessage userMessage;

    /**
     * （助手消息） - 模型生成的响应，包括文本内容、工具调用和元数据 Tool
     * text: 消息的文本内容
     * metadata: 消息的元数据映射
     * toolCalls: 模型进行的工具调用列表
     * media: 媒体内容列表（如果有）
     */
    private AssistantMessage assistantMessage;

    /**
     * （工具响应消息） - 表示工具调用的输出
     */
    private ToolResponseMessage toolResponseMessage;

    /**
     * @param systemMessage
     * @param userMessage
     * @param assistantMessage
     * @param toolResponseMessage
     * @return
     */
    public static List<Message> getAbstractMessage(SystemMessage systemMessage, UserMessage userMessage, AssistantMessage assistantMessage,
            ToolResponseMessage toolResponseMessage) {

        List<Message> messageList = new ArrayList<>();

        if (systemMessage != null) {
            messageList.add(systemMessage);
        }

        if (userMessage != null) {
            messageList.add(userMessage);
        }

        if (assistantMessage != null) {
            messageList.add(assistantMessage);
        }

        if (toolResponseMessage != null) {
            messageList.add(toolResponseMessage);
        }

        if (messageList.isEmpty()) {
            throw new IllegalArgumentException("参数不能为空");
        }

        return messageList;
    }
}
