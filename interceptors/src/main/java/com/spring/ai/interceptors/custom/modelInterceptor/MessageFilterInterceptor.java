package com.spring.ai.interceptors.custom.modelInterceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

/**
 * 控制发送给 LLM 的消息列表
 * 过滤或修改消息
 * 添加上下文或摘要
 * 压缩长对话
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/7
 */
class MessageFilterInterceptor extends ModelInterceptor {

    /**
     * 最大消息数
     */
    private final int maxMessages;

    public MessageFilterInterceptor(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        List<Message> messages = request.getMessages();

        // 只保留最近的N条消息
        if (messages.size() > maxMessages) {
            List<Message> filtered = new ArrayList<>();

            // 添加系统消息
            messages.stream()
                    .filter(m -> m instanceof SystemMessage)
                    .findFirst()
                    .ifPresent(filtered::add);

            // 添加最近的消息
            int startIndex = Math.max(0, messages.size() - maxMessages + 1);
            filtered.addAll(messages.subList(startIndex, messages.size()));

            messages = filtered;
        }

        ModelRequest enhancedRequest = ModelRequest.builder(request)
                .messages(messages)
                .build();

        return handler.call(enhancedRequest);
    }

    @Override
    public String getName() {
        return "MessageFilterInterceptor";
    }
}