package com.spring.ai.hooks.custom.messagesModelHook;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

/**
 * REPLACE 策略：替换所有现有消息； APPEND 策略：将新消息追加到现有消息列表。
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@HookPositions({HookPosition.BEFORE_MODEL})
public class ContextEnhancementHook extends MessagesModelHook {

    @Override
    public String getName() {
        return "context_enhancement";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        // 示例 1: 使用 REPLACE 策略替换所有消息
        List<Message> newMessages = new ArrayList<>();
        newMessages.add(new SystemMessage("你是一个专业的助手"));
        newMessages.addAll(previousMessages);
        return new AgentCommand(newMessages, UpdatePolicy.REPLACE);

        // 示例 2: 使用 APPEND 策略追加消息
        // List<Message> additionalMessages = List.of(
        //     new UserMessage("请记住：保持友好和专业")
        // );
        // return new AgentCommand(additionalMessages, UpdatePolicy.APPEND);
    }
}
