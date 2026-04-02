package com.spring.ai.hooks.custom.messagesModelHook;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import java.util.List;
import org.springframework.ai.chat.messages.Message;

/**
 * 也支持通过 JumpTo 实现提前退出
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@HookPositions({HookPosition.BEFORE_MODEL})
public class EarlyExitHook extends MessagesModelHook {

    @Override
    public String getName() {
        return "early_exit";
    }

    @Override
    public List<JumpTo> canJumpTo() {
        return List.of(JumpTo.end);
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        // 检查某些条件，如果满足则提前退出
        if (shouldExit(previousMessages)) {
            return new AgentCommand(JumpTo.end, previousMessages);
        }
        return new AgentCommand(previousMessages);
    }

    private boolean shouldExit(List<Message> messages) {
        // 实现你的退出逻辑
        return false;
    }
}