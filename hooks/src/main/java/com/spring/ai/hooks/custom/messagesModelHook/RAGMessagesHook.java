package com.spring.ai.hooks.custom.messagesModelHook;

import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_MODULE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.MODULE_NAME;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 基于 MessagesModelHook 的 RAG 检索增强 Hook。
 * 在模型调用前读取用户最后一条问题，执行向量检索，并将检索结果整理为系统上下文注入到消息列表中。
 * 为了与 vectorStore 模块保持一致，这里默认只检索当前模块写入的向量数据。
 *
 * @author zhouqi
 * @since 2026/4/10
 */
@Component
@HookPositions({HookPosition.BEFORE_MODEL})
public class RAGMessagesHook extends MessagesModelHook {

    private static final Logger log = LoggerFactory.getLogger(RAGMessagesHook.class);

    /**
     * 默认召回文档数量。
     */
    private static final int DEFAULT_TOP_K = 4;

    /**
     * 注入到提示词中的最大文档数，避免上下文膨胀。
     */
    private static final int MAX_CONTEXT_DOCUMENTS = 4;

    /**
     * 单条文档写入提示词时的最大字符数，避免单个切片过长。
     */
    private static final int MAX_DOCUMENT_CHARS = 800;


    private final VectorStore vectorStore;

    public RAGMessagesHook(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }


    @Override
    public String getName() {
        return "rag_messages_hook";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        String userQuestion = extractLatestUserQuestion(previousMessages);
        if (!StringUtils.hasText(userQuestion)) {
            return new AgentCommand(previousMessages);
        }

        List<Document> documents = retrieveDocuments(userQuestion.trim());
        if (documents.isEmpty()) {
            return new AgentCommand(previousMessages);
        }

        String ragContext = buildRagContext(documents);
        if (!StringUtils.hasText(ragContext)) {
            return new AgentCommand(previousMessages);
        }

        List<Message> enhancedMessages = new ArrayList<>(previousMessages.size() + 1);
        enhancedMessages.add(new SystemMessage(buildSystemPrompt(ragContext)));
        enhancedMessages.addAll(previousMessages);
        return new AgentCommand(enhancedMessages, UpdatePolicy.REPLACE);
    }

    /**
     * 按照 vectorStore 模块现有元数据规则执行相似度检索。
     */
    private List<Document> retrieveDocuments(String userQuestion) {
        if (vectorStore == null) {
            log.warn("RAG hook skipped because VectorStore bean is not injected. Please obtain RAGMessagesHook from Spring instead of manual new.");
            return List.of();
        }
        try {
            FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
            SearchRequest request = SearchRequest.builder()
                    .query(userQuestion)
                    .topK(DEFAULT_TOP_K)
                    .filterExpression(filterExpressionBuilder.eq(METADATA_MODULE, MODULE_NAME).build())
                    .build();

            List<Document> documents = vectorStore.similaritySearch(request);

            return documents.stream()
                    .filter(Objects::nonNull)
                    .filter(document -> StringUtils.hasText(document.getText()))
                    .limit(MAX_CONTEXT_DOCUMENTS)
                    .toList();
        } catch (RuntimeException exception) {
            log.warn("RAG hook skipped because vector search failed: {}", exception.getMessage(), exception);
            return List.of();
        }
    }

    /**
     * 提取最后一条用户问题，避免把系统消息或工具消息拿去做检索。
     */
    private String extractLatestUserQuestion(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message instanceof UserMessage userMessage && StringUtils.hasText(userMessage.getText())) {
                return userMessage.getText();
            }
        }
        return null;
    }

    /**
     * 将召回结果压缩成可控长度的上下文文本，降低提示词膨胀风险。
     */
    private String buildRagContext(List<Document> documents) {
        StringBuilder contextBuilder = new StringBuilder();
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            String content = abbreviate(document.getText(), MAX_DOCUMENT_CHARS);
            if (!StringUtils.hasText(content)) {
                continue;
            }
            contextBuilder.append("资料").append(index + 1).append(":\n");
            contextBuilder.append(content).append("\n\n");
        }
        return contextBuilder.toString().trim();
    }

    /**
     * 构建注入模型的系统提示词，明确要求优先依据检索内容回答。
     */
    private String buildSystemPrompt(String ragContext) {
        return """
                你正在使用检索增强生成（RAG）模式回答问题。
                请优先依据下面提供的资料回答用户问题，不要编造资料中不存在的事实。
                如果资料不足以支持结论，请明确说明“根据当前检索结果无法确定”。

                检索资料：
                %s
                """.formatted(ragContext);
    }

    private String abbreviate(String text, int maxChars) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars) + "...";
    }
}
