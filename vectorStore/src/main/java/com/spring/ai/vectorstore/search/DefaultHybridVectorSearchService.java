package com.spring.ai.vectorstore.search;

import com.spring.ai.vectorstore.config.VectorStoreProperties;
import com.spring.ai.vectorstore.rerank.DocumentReranker;
import com.spring.ai.vectorstore.store.VectorStoreGateway;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 默认混合检索服务，使用 RRF 合并向量召回和关键词召回，并支持重排。
 */
@Service
public class DefaultHybridVectorSearchService implements HybridVectorSearchService {

    private final VectorStoreProperties properties;
    private final VectorStoreGateway vectorStoreGateway;
    private final DocumentReranker documentReranker;

    public DefaultHybridVectorSearchService(
            VectorStoreProperties properties,
            VectorStoreGateway vectorStoreGateway,
            DocumentReranker documentReranker
    ) {
        this.properties = properties;
        this.vectorStoreGateway = vectorStoreGateway;
        this.documentReranker = documentReranker;
    }

    @Override
    public List<Document> search(SearchRequest searchRequest, String fileName) {
        VectorStoreProperties.HybridSearch hybrid = properties.getHybridSearch();
        if (!hybrid.isEnabled()) {
            return vectorStoreGateway.similaritySearch(searchRequest);
        }

        List<List<Document>> rankedLists = new ArrayList<>();
        if (hybrid.isVectorEnabled()) {
            rankedLists.add(vectorSearch(searchRequest, hybrid));
        }
        if (hybrid.isKeywordEnabled()) {
            List<Document> keywordDocuments = keywordSearch(searchRequest.getQuery(), fileName, searchRequest.getTopK(), hybrid);
            if (!keywordDocuments.isEmpty()) {
                rankedLists.add(keywordDocuments);
            }
        }
        List<Document> fusedDocuments = reciprocalRankFusion(rankedLists, hybrid.getRrfK(), searchRequest.getTopK(), hybrid);
        if (hybrid.isRerankEnabled()) {
            fusedDocuments = documentReranker.rerank(searchRequest.getQuery(), fusedDocuments);
        }
        return fusedDocuments.stream().limit(searchRequest.getTopK()).toList();
    }

    private List<Document> vectorSearch(SearchRequest searchRequest, VectorStoreProperties.HybridSearch hybrid) {
        int candidateTopK = Math.max(searchRequest.getTopK(), searchRequest.getTopK() * Math.max(1, hybrid.getVectorCandidateMultiplier()));
        SearchRequest.Builder builder = SearchRequest.from(searchRequest).topK(candidateTopK);
        return vectorStoreGateway.similaritySearch(builder.build());
    }

    private List<Document> keywordSearch(
            String query,
            String fileName,
            int topK,
            VectorStoreProperties.HybridSearch hybrid
    ) {
        Set<String> queryTerms = tokenize(query);
        int candidateSize = Math.max(topK, topK * Math.max(1, hybrid.getKeywordCandidateMultiplier()));
        try {
            return vectorStoreGateway.listDocuments(fileName).stream()
                    .map(document -> withScore(document, keywordScore(queryTerms, document)))
                    .filter(document -> document.getScore() != null && document.getScore() > 0D)
                    .sorted(Comparator.comparing(this::documentScore).reversed())
                    .limit(candidateSize)
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private List<Document> reciprocalRankFusion(
            List<List<Document>> rankedLists,
            int rrfK,
            int topK,
            VectorStoreProperties.HybridSearch hybrid
    ) {
        Map<String, FusionItem> fusionItems = new LinkedHashMap<>();
        for (List<Document> rankedList : rankedLists) {
            for (int index = 0; index < rankedList.size(); index++) {
                Document document = rankedList.get(index);
                String key = documentKey(document);
                FusionItem fusionItem = fusionItems.computeIfAbsent(key, ignored -> new FusionItem(document, 0D));
                fusionItem.score += 1D / (Math.max(1, rrfK) + index + 1D);
            }
        }
        int rerankCandidateSize = Math.max(topK, topK * Math.max(1, hybrid.getRerankCandidateMultiplier()));
        return fusionItems.values().stream()
                .map(fusionItem -> withFusionScore(fusionItem.document, fusionItem.score))
                .sorted(Comparator.comparing(this::documentScore).reversed())
                .limit(rerankCandidateSize)
                .toList();
    }

    private double keywordScore(Set<String> queryTerms, Document document) {
        if (queryTerms.isEmpty() || !StringUtils.hasText(document.getText())) {
            return 0D;
        }
        Set<String> documentTerms = tokenize(document.getText());
        long hitCount = queryTerms.stream().filter(documentTerms::contains).count();
        return hitCount / (double) queryTerms.size();
    }

    private Document withScore(Document document, double score) {
        Map<String, Object> metadata = new HashMap<>(document.getMetadata());
        metadata.put("keywordScore", score);
        return Document.builder()
                .id(document.getId())
                .text(document.getText())
                .metadata(metadata)
                .score(score)
                .build();
    }

    private Document withFusionScore(Document document, double score) {
        Map<String, Object> metadata = new HashMap<>(document.getMetadata());
        metadata.put("rrfScore", score);
        return Document.builder()
                .id(document.getId())
                .text(document.getText())
                .metadata(metadata)
                .score(score)
                .build();
    }

    private double documentScore(Document document) {
        return document.getScore() == null ? 0D : document.getScore();
    }

    private String documentKey(Document document) {
        return StringUtils.hasText(document.getId()) ? document.getId() : document.getText();
    }

    private Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        return new HashSet<>(Arrays.stream(text.toLowerCase().split("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}]+"))
                .filter(StringUtils::hasText)
                .toList());
    }

    private static class FusionItem {
        private final Document document;
        private double score;

        private FusionItem(Document document, double score) {
            this.document = document;
            this.score = score;
        }
    }
}


