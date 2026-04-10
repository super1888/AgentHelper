package com.spring.ai.interceptors.custom.modelInterceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * 过 ModelInterceptor 检索文档后附加到 systemPrompt：
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/10
 */
@Slf4j
public class RAGModelInterceptor extends ModelInterceptor {

    private final VectorStore vectorStore;
    private static final int TOP_K = 5;

    public RAGModelInterceptor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 从用户消息中提取查询
        String userQuery = extractUserQuery(request);
        if (userQuery.isEmpty()) {
            return handler.call(request);
        }

        // Step 1: 检索相关文档
        List<Document> relevantDocs = vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.builder()
                        .query(userQuery)
                        .topK(TOP_K)
                        .build()
        );

        // Step 2: 构建上下文
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining(" "));
        // Step 3: 增强 systemPrompt
        String enhancedSystemPrompt = String.format("""
                你是一个有用的助手。基于以下上下文回答问题。
                如果上下文中没有相关信息，请说明你不知道。
                                
                上下文：
                %s
                """, context);

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

        // 调用处理器
        return handler.call(enhancedRequest);
    }

    private String extractUserQuery(ModelRequest request) {
        // 从消息列表中提取用户查询
        return request.getMessages().stream()
                .filter(msg -> msg instanceof org.springframework.ai.chat.messages.UserMessage)
                .map(msg -> ((org.springframework.ai.chat.messages.UserMessage) msg).getText())
                .reduce((first, second) -> second) // 获取最后一个用户消息
                .orElse("");
    }

    @Override
    public String getName() {
        return "rag_model_interceptor";
    }
}
