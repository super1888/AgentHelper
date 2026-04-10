package com.spring.ai.hooks.custom.agentHook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * 在 Agent 开始时检索文档（只执行一次）
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/10
 */
@HookPositions({HookPosition.BEFORE_AGENT})
class RAGAgentHook extends AgentHook {

    private final VectorStore vectorStore;
    private static final int TOP_K = 5;
    private static final String RAG_CONTEXT_KEY = "rag_context";

    public RAGAgentHook(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public String getName() {
        return "rag_agent_hook";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
        // 从状态中提取用户问题
        Optional<Object> messagesOpt = state.value("messages");
        if (messagesOpt.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        @SuppressWarnings("unchecked")
        List<Message> messages =
                (List<org.springframework.ai.chat.messages.Message>) messagesOpt.get();

        // 提取最后一个用户消息作为查询
        String userQuery = messages.stream()
                .filter(msg -> msg instanceof org.springframework.ai.chat.messages.UserMessage)
                .map(msg -> ((org.springframework.ai.chat.messages.UserMessage) msg).getText())
                .reduce((first, second) -> second) // 获取最后一个
                .orElse("");

        if (userQuery.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }

        // Step 1: 检索相关文档（只执行一次，在整个 Agent 执行过程中）
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
                        config.metadata().ifPresent(meta -> {
                            meta.put(RAG_CONTEXT_KEY, context);
                        });

        // Step 3: 将检索到的上下文存储到状态中，供后续 ModelInterceptor 使用
        // 存储到 state 中，ModelInterceptor 可以通过 request.getContext() 访问
        return CompletableFuture.completedFuture(Map.of());
    }
}