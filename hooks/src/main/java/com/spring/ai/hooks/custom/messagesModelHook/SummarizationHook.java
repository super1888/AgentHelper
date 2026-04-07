package com.spring.ai.hooks.custom.messagesModelHook;

import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel.ChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * 消息摘要Hook
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/7
 */
@HookPositions({HookPosition.BEFORE_MODEL})
class SummarizationHook extends MessagesModelHook {
    private final ChatModel summarizationModel;
    private final int triggerLength;

    public SummarizationHook(ChatModel model, int triggerLength) {
        this.summarizationModel = model;
        this.triggerLength = triggerLength;
    }

    @Override
    public String getName() {
        return "summarization_hook";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        if (previousMessages.size() > triggerLength) {
            // 生成对话摘要
            String summary = generateSummary(previousMessages);

            // 查找是否已存在 SystemMessage（保留它，不修改）
            SystemMessage existingSystemMessage = null;
            for (Message msg : previousMessages) {
                if (msg instanceof SystemMessage) {
                    existingSystemMessage = (SystemMessage) msg;
                    break;
                }
            }

            // 创建包含摘要的上下文消息（使用 UserMessage 而不是 SystemMessage）
            // 这样可以将摘要作为对话上下文的一部分，而不修改系统提示
            UserMessage summaryContextMessage = new UserMessage(
                    "【上下文摘要】之前的对话摘要：" + summary
            );

            // 保留最近的几条消息
            int recentCount = Math.min(5, previousMessages.size());
            List<Message> recentMessages = previousMessages.subList(
                    previousMessages.size() - recentCount,
                    previousMessages.size()
            );

            // 构建新的消息列表
            List<Message> newMessages = new ArrayList<>();

            // 保留原有的 SystemMessage（如果存在）
            if (existingSystemMessage != null) {
                newMessages.add(existingSystemMessage);
            }

            // 添加摘要上下文消息
            newMessages.add(summaryContextMessage);

            // 添加最近的消息，排除旧的 SystemMessage（如果存在）
            for (Message msg : recentMessages) {
                if (msg != existingSystemMessage) {
                    newMessages.add(msg);
                }
            }

            // 使用 REPLACE 策略替换消息列表
            return new AgentCommand(newMessages, UpdatePolicy.REPLACE);
        }

        // 如果消息数量未超过阈值，返回原始消息（不进行修改）
        return new AgentCommand(previousMessages);
    }

    private String generateSummary(List<Message> messages) {
        // 使用另一个模型生成摘要
        String conversation = messages.stream()
                .map(Message::getText)
                .collect(Collectors.joining(" "));

        // 简化示例：返回固定摘要
        return "之前讨论了多个主题...";
    }
}
