package com.spring.ai.hooks.custom.modelHook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.ai.chat.messages.Message;

/**
 * 使用 ModelHook 实现消息修剪（可访问状态）
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@HookPositions({HookPosition.BEFORE_MODEL})
public class AdvancedMessageTrimmingHook extends ModelHook {

    private static final int MAX_MESSAGES = 10;
    private static final String TRIM_COUNT_KEY = "trim_count";

    @Override
    public String getName() {
        return "advanced_message_trimming";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        // 可以访问完整状态
        Optional<Object> messagesOpt = state.value("messages");
        if (messagesOpt.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        List<Message> messages = (List<Message>) messagesOpt.get();

        // 可以访问和更新自定义状态
        int trimCount = (Integer) state.value(TRIM_COUNT_KEY).orElse(0);

        if (messages.size() > MAX_MESSAGES) {
            List<Message> trimmed = messages.subList(
                    messages.size() - MAX_MESSAGES,
                    messages.size()
            );

            // 可以同时更新消息和自定义状态
            return CompletableFuture.completedFuture(Map.of(
                    "messages", ReplaceAllWith.of(trimmed),
                    TRIM_COUNT_KEY, trimCount + 1  // 记录修剪次数
            ));
        }

        return CompletableFuture.completedFuture(Map.of());
    }
}

