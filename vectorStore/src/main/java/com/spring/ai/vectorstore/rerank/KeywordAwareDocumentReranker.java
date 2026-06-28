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
 * 默认轻量重排器。
 *
 * <p>核心作用：</p>
 * <p>1. RRF 融合后得到的是多路召回的综合排序，但不一定完全贴合用户问题。</p>
 * <p>2. 该重排器会再次计算“用户问题词项”和“候选文档词项”的覆盖度。</p>
 * <p>3. 最终分数由词项覆盖度和原始融合分数加权得到，并写入 metadata.rerankScore。</p>
 */
@Component
public class KeywordAwareDocumentReranker implements DocumentReranker {

    /**
     * 对候选文档进行重排。
     *
     * @param query 用户原始问题，用于拆分关键词并计算候选文档的词项覆盖度。
     * @param candidates RRF 融合后的候选文档列表，通常数量大于最终 topK。
     * @return 按 rerankScore 从高到低排序后的文档列表。
     */
    @Override
    public List<Document> rerank(String query, List<Document> candidates) {
        // queryTerms：用户问题拆分后的词项集合，支持中文、英文和数字。
        Set<String> queryTerms = tokenize(query);
        return candidates.stream()
                // 重新计算每个文档的 rerankScore，并写入 Document.score。
                .map(document -> withRerankScore(document, rerankScore(queryTerms, document)))
                .sorted(Comparator.comparing(this::scoreOf).reversed())
                .toList();
    }

    private Document withRerankScore(Document document, double rerankScore) {
        // 复制原始元数据，保留 rrfScore、keywordScore 等上游分数，再追加 rerankScore。
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
        // documentTerms：候选文档正文拆分后的词项集合。
        Set<String> documentTerms = tokenize(document.getText());
        // hitCount：用户问题词项中有多少出现在文档中。
        long hitCount = queryTerms.stream().filter(documentTerms::contains).count();
        // lexicalScore：词项覆盖比例，越接近 1 说明文档越匹配用户问题关键词。
        double lexicalScore = hitCount / (double) queryTerms.size();
        // originalScore：上游传入的分数，通常是 RRF 融合分数。
        double originalScore = document.getScore() == null ? 0D : document.getScore();
        // 当前策略更重视词项覆盖度，同时保留一部分上游融合排序贡献。
        return lexicalScore * 0.6D + originalScore * 0.4D;
    }

    private double scoreOf(Document document) {
        return document.getScore() == null ? 0D : document.getScore();
    }

    private Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        // 使用非中文、非字母、非数字字符作为分隔符，兼容自然语言和代码标识符。
        return new HashSet<>(Arrays.stream(text.toLowerCase().split("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}]+"))
                .filter(StringUtils::hasText)
                .toList());
    }
}