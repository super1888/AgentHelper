package com.spring.ai.interceptors.custom.modelInterceptor;


import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

/**
 * 模型拦截器，根据对话长度调整系统提示
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/7
 */
class StateAwarePromptInterceptor extends ModelInterceptor {

    /**
     * 基本系统提示
     */
    private final String basePrompt;

    /**
     * 扩展系统提示
     */
    private final String enhancePrompt;


    public StateAwarePromptInterceptor(String basePrompt, String enhancePrompt) {
        this.basePrompt = basePrompt;
        this.enhancePrompt = enhancePrompt;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        List<Message> messages = request.getMessages();
        int messageCount = messages.size();

        // 基础提示
        String basePrompt = this.basePrompt;

        // 根据消息数量调整提示
        if (messageCount > 10) {
            basePrompt += this.enhancePrompt;
        }

        // 更新系统消息（参考 TodoListInterceptor 的实现方式）
        SystemMessage enhancedSystemMessage;
        if (request.getSystemMessage() == null) {
            enhancedSystemMessage = new SystemMessage(basePrompt);
        } else {
            enhancedSystemMessage = new SystemMessage(
                    request.getSystemMessage().getText() + " " + basePrompt
            );
        }

        // 创建增强的请求
        ModelRequest enhancedRequest = ModelRequest.builder(request)
                .systemMessage(enhancedSystemMessage)
                .build();

        // 调用处理器
        return handler.call(enhancedRequest);
    }

    @Override
    public String getName() {
        return "StateAwarePromptInterceptor";
    }
}
