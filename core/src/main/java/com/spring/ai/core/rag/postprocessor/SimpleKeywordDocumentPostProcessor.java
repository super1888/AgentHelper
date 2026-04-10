package com.spring.ai.core.rag.postprocessor;

import java.util.List;
import java.util.Locale;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 简单文档后处理器。
 *
 * <p>该类主要用于学习模块化 RAG 的 Post-Retrieval 阶段，处理逻辑尽量写得直白：
 * 1. 如果配置了关键字，则只保留正文中包含关键字的文档
 * 2. 对正文做统一截断，避免上下文过长
 * 3. 保留原始 metadata，方便继续追踪来源</p>
 */
public class SimpleKeywordDocumentPostProcessor implements DocumentPostProcessor {

    private final List<String> requiredKeywords;
    private final int maxChars;

    public SimpleKeywordDocumentPostProcessor(List<String> requiredKeywords, int maxChars) {
        this.requiredKeywords = requiredKeywords;
        this.maxChars = maxChars;
    }

    @Override
    public List<Document> process(Query query, List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return List.of();
        }

        return documents.stream()
                .filter(this::containsRequiredKeyword)
                .map(this::truncateDocument)
                .toList();
    }

    private boolean containsRequiredKeyword(Document document) {
        if (CollectionUtils.isEmpty(requiredKeywords)) {
            return true;
        }
        String text = document.getText();
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        return requiredKeywords.stream()
                .filter(StringUtils::hasText)
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(lowerText::contains);
    }

    private Document truncateDocument(Document document) {
        String text = document.getText();
        if (!StringUtils.hasText(text) || text.length() <= maxChars) {
            return document;
        }
        return Document.builder()
                .id(document.getId())
                .score(document.getScore())
                .metadata(document.getMetadata())
                .text(text.substring(0, maxChars) + "...")
                .build();
    }
}
