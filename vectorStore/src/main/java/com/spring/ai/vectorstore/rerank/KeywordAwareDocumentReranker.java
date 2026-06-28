package com.spring.ai.vectorstore.rerank;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 默认轻量重排器，使用词项覆盖度和原始分数进行二次排序。
 */
@Component
public class KeywordAwareDocumentReranker implements DocumentReranker {

    @Override
    public List<Document> rerank(String query, List<Document> candidates) {
        Set<String> queryTerms = tokenize(query);
        return candidates.stream()
                .map(document -> withRerankScore(document, rerankScore(queryTerms, document)))
                .sorted(Comparator.comparing(this::scoreOf).reversed())
                .toList();
    }

    private Document withRerankScore(Document document, double rerankScore) {
        Map<String, Object> metadata = new LinkedHashMap<>(document.getMetadata());
        metadata.put("rerankScore", rerankScore);
        return Document.builder()
                .id(document.getId())
                .text(document.getText())
                .metadata(metadata)
                .score(rerankScore)
                .build();
    }

    private double rerankScore(Set<String> queryTerms, Document document) {
        if (queryTerms.isEmpty()) {
            return document.getScore() == null ? 0D : document.getScore();
        }
        Set<String> documentTerms = tokenize(document.getText());
        long hitCount = queryTerms.stream().filter(documentTerms::contains).count();
        double lexicalScore = hitCount / (double) queryTerms.size();
        double originalScore = document.getScore() == null ? 0D : document.getScore();
        return lexicalScore * 0.6D + originalScore * 0.4D;
    }

    private double scoreOf(Document document) {
        return document.getScore() == null ? 0D : document.getScore();
    }

    private Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        return new HashSet<>(Arrays.stream(text.toLowerCase().split("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}]+"))
                .filter(StringUtils::hasText)
                .toList());
    }
}
