package com.spring.ai.interceptors.custom.modelInterceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import java.util.List;
import org.springframework.ai.chat.messages.Message;

/**
 * 响应格式控制
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/7
 */
class DynamicFormatInterceptor extends ModelInterceptor {
    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 根据请求内容决定输出格式
        String outputSchema = determineOutputSchema(request);

        // 在消息中添加格式说明
        List<Message> updatedMessages = addFormatInstructions(
                request.getMessages(),
                outputSchema
        );

        ModelRequest enhancedRequest = ModelRequest.builder(request)
                .messages(updatedMessages)
                .build();

        return handler.call(enhancedRequest);
    }

    private String determineOutputSchema(ModelRequest request) {
        // 实现输出格式决定逻辑
        return "";
    }

    private List<Message> addFormatInstructions(List<Message> messages, String schema) {
        // 实现格式说明添加逻辑
        return messages;
    }

    @Override
    public String getName() {
        return "DynamicFormatInterceptor";
    }
}
