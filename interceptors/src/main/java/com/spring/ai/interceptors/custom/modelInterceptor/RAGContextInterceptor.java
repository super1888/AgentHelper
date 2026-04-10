package com.spring.ai.interceptors.custom.modelInterceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import java.util.Map;
import org.springframework.ai.chat.messages.SystemMessage;

/**
 * 在模型调用时使用存储的上下文
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/10
 */
class RAGContextInterceptor extends ModelInterceptor {

    private static final String RAG_CONTEXT_KEY = "rag_context";

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 从请求上下文中获取检索到的 RAG 上下文
        // RAG 上下文在 AgentHook 的 beforeAgent 中已经存储到状态中
        Map<String, Object> context = request.getContext();
        String ragContext = (String) context.get(RAG_CONTEXT_KEY);

        if (ragContext == null || ragContext.isEmpty()) {
            // 如果没有检索到上下文，直接调用处理器
            return handler.call(request);
        }

        // 增强 systemPrompt
        String enhancedSystemPrompt = String.format("""
                你是一个有用的助手。基于以下上下文回答问题。
                如果上下文中没有相关信息，请说明你不知道。
                          
                上下文：
                %s
                """, ragContext);

        // 合并原有的 systemPrompt 和检索到的上下文
        SystemMessage enhancedSystemMessage;
        if (request.getSystemMessage() == null) {
            enhancedSystemMessage = new SystemMessage(enhancedSystemPrompt);
        } else {
            enhancedSystemMessage = new SystemMessage(
                    request.getSystemMessage().getText() + " " + enhancedSystemPrompt
            );
        }

        // 创建增强的请求
        ModelRequest enhancedRequest = ModelRequest.builder(request)
                .systemMessage(enhancedSystemMessage)
                .build();

        return handler.call(enhancedRequest);
    }

    @Override
    public String getName() {
        return "rag_context_interceptor";
    }
}

