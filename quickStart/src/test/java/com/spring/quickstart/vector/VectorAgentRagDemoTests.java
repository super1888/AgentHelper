package com.spring.quickstart.vector;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.spring.ai.common.constants.VectorStoreManagerConstants;
import com.spring.ai.core.facotry.GetChatModel;
import com.spring.ai.vectorstore.search.HybridVectorSearchService;
import com.spring.quickstart.QuickStartApplication;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

/**
 * Agent 调用向量库的演示测试。
 * 重点演示：检索增强、提示词防幻觉约束、无依据拒答、回答后幻觉校验。
 */
@SpringBootTest(classes = QuickStartApplication.class)
class VectorAgentRagDemoTests {

    private static final String UNKNOWN_ANSWER = "未在知识库中找到可靠依据，无法回答。";
    private static final double MIN_CONTEXT_SIMILARITY = 0.45D;

    @Resource
    private HybridVectorSearchService hybridVectorSearchService;

    @Resource
    private GetChatModel getChatModel;

    /**
     * Agent 使用向量库回答问题，并在提示词和后处理阶段降低幻觉。
     */
    @Test
    @Disabled("该 demo 依赖本地向量库、Embedding 模型和聊天模型配置，手动打开后运行")
    @DisplayName("Agent 应该基于向量库上下文回答并处理幻觉")
    void shouldAnswerWithVectorContextAndHallucinationGuard() {
        String userQuestion = "面向对象的协议是什么？";

        List<Document> retrievedDocuments = retrieveKnowledge(userQuestion, 4);
        String context = buildContext(retrievedDocuments);
        String prompt = buildGroundedPrompt(userQuestion, context);

        ChatClient chatClient = ChatClient.builder(getChatModel.creatDashScopeChatModel()).build();
        String answer = chatClient.prompt()
                .system(buildSystemPrompt())
                .user(prompt)
                .call()
                .content();

        String safeAnswer = handleHallucination(userQuestion, answer, retrievedDocuments);
        System.out.println("混合检索上下文：\n" + context);
        System.out.println("模型原始回答：\n" + answer);
        System.out.println("安全处理后回答：\n" + safeAnswer);

        assertNotNull(safeAnswer);
    }

    /**
     * 使用混合检索获取当前模块知识，内部执行向量检索、关键词检索、RRF 融合和可选 Rerank。
     */
    private List<Document> retrieveKnowledge(String query, int topK) {
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(MIN_CONTEXT_SIMILARITY)
                .filterExpression(filterExpressionBuilder
                        .eq(VectorStoreManagerConstants.METADATA_MODULE, VectorStoreManagerConstants.MODULE_NAME)
                        .build())
                .build();
        return hybridVectorSearchService.search(searchRequest, null);
    }

    /**
     * 构造带来源编号的上下文，便于回答时引用证据。
     */
    private String buildContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }
        StringBuilder contextBuilder = new StringBuilder();
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            contextBuilder.append("[资料").append(index + 1).append("]\n")
                    .append("来源：").append(resolveSource(document)).append('\n')
                    .append("最终分数：").append(document.getScore()).append('\n')
                    .append("融合信息：").append(resolveHybridScoreInfo(document)).append('\n')
                    .append(normalizeContextText(document.getText()))
                    .append("\n\n");
        }
        return contextBuilder.toString().trim();
    }

    /**
     * 系统提示词用于强约束 Agent 只能基于检索上下文回答。
     */
    private String buildSystemPrompt() {
        return """
                你是一个严谨的知识库问答 Agent。
                必须遵守以下规则：
                1. 只能依据用户提供的【检索上下文】回答，不允许使用未给出的外部知识。
                2. 如果上下文没有明确答案，必须回答：未在知识库中找到可靠依据，无法回答。
                3. 不允许编造数据、定义、流程、来源、文件名或结论。
                4. 回答中必须给出引用，例如：[资料1]、[资料2]。
                5. 如果多个资料冲突，说明冲突点，不要自行裁决。
                6. 输出使用中文，先给结论，再列依据。
                """;
    }

    /**
     * 用户提示词显式注入检索上下文和问题，减少模型自由发挥空间。
     */
    private String buildGroundedPrompt(String userQuestion, String context) {
        return """
                【检索上下文】
                %s

                【用户问题】
                %s

                【回答要求】
                - 只回答上下文能支持的内容。
                - 每个关键结论后标注资料编号。
                - 如果上下文为空或证据不足，直接输出：%s
                """.formatted(StringUtils.hasText(context) ? context : "无可用上下文", userQuestion, UNKNOWN_ANSWER);
    }

    /**
     * 对回答做简单幻觉处理：无检索结果、无引用、疑似脱离上下文时降级拒答。
     */
    private String handleHallucination(String question, String answer, List<Document> evidenceDocuments) {
        if (evidenceDocuments == null || evidenceDocuments.isEmpty()) {
            return UNKNOWN_ANSWER;
        }
        if (!StringUtils.hasText(answer)) {
            return UNKNOWN_ANSWER;
        }
        if (answer.contains(UNKNOWN_ANSWER)) {
            return UNKNOWN_ANSWER;
        }
        if (!answer.matches("(?s).*\\[资料\\d+].*")) {
            return UNKNOWN_ANSWER + " 原因：回答未引用检索资料。";
        }
        if (!isAnswerGrounded(question, answer, evidenceDocuments)) {
            return UNKNOWN_ANSWER + " 原因：回答内容与检索上下文重合度过低，疑似幻觉。";
        }
        return answer;
    }

    /**
     * 使用轻量词项重合度判断回答是否被上下文支撑。
     */
    private boolean isAnswerGrounded(String question, String answer, List<Document> evidenceDocuments) {
        String evidenceText = evidenceDocuments.stream()
                .map(Document::getText)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
        List<String> answerTerms = tokenize(answer).stream()
                .filter(term -> !question.contains(term))
                .filter(term -> term.length() >= 2)
                .toList();
        if (answerTerms.isEmpty()) {
            return true;
        }
        long groundedTerms = answerTerms.stream()
                .filter(evidenceText::contains)
                .count();
        return groundedTerms / (double) answerTerms.size() >= 0.25D;
    }

    private List<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return java.util.Arrays.stream(text.replaceAll("[^\\p{IsHan}a-zA-Z0-9]", " ").split("\\s+"))
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String resolveHybridScoreInfo(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return "无融合分数元数据";
        }
        return "rrfScore=" + metadata.getOrDefault("rrfScore", "无")
                + ", keywordScore=" + metadata.getOrDefault("keywordScore", "无")
                + ", rerankScore=" + metadata.getOrDefault("rerankScore", "无");
    }

    private String resolveSource(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return "未知来源";
        }
        Object fileName = metadata.get(VectorStoreManagerConstants.METADATA_FILE_NAME);
        Object source = metadata.get(VectorStoreManagerConstants.METADATA_SOURCE);
        if (fileName != null) {
            return String.valueOf(fileName);
        }
        return source == null ? "未知来源" : String.valueOf(source);
    }

    private String normalizeContextText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.length() <= 1200 ? text : text.substring(0, 1200) + "...";
    }
}
