package com.spring.ai.vectorstore.store;

import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_FILE_NAME;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_MODULE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.MODULE_NAME;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.vectorstore.config.VectorStoreProperties;
import com.spring.ai.vectorstore.config.VectorStoreProperties.AnnAlgorithm;
import com.spring.ai.vectorstore.exception.VectorStoreException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.util.StringUtils;

/**
 * 本地 FAISS 风格向量存储网关，提供单机近似向量检索和 JSON 持久化能力。
 */
public class LocalFaissVectorStoreGateway implements VectorStoreGateway {

    private final EmbeddingModel embeddingModel;
    private final VectorStoreProperties properties;
    private final ObjectMapper objectMapper;
    private final Path indexPath;
    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public LocalFaissVectorStoreGateway(
            EmbeddingModel embeddingModel,
            VectorStoreProperties properties,
            ObjectMapper objectMapper
    ) {
        this.embeddingModel = embeddingModel;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.indexPath = Path.of(properties.getFaiss().getIndexPath());
        loadIndex();
    }

    @Override
    public void ensureReady() {
        if (embeddingModel == null) {
            throw VectorStoreException.badRequest("FAISS 本地存储需要可用的 EmbeddingModel");
        }
    }

    @Override
    public void add(List<Document> documents) {
        ensureReady();
        List<Entry> newEntries = documents.stream()
                .filter(document -> StringUtils.hasText(document.getText()))
                .map(this::toEntry)
                .toList();
        entries.addAll(newEntries);
        saveIndex();
    }

    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        ensureReady();
        float[] queryVector = normalizeIfNeeded(embeddingModel.embed(searchRequest.getQuery()));
        int candidateSize = resolveCandidateSize(searchRequest.getTopK());
        return entries.stream()
                .filter(entry -> MODULE_NAME.equals(asString(entry.metadata().get(METADATA_MODULE))))
                .filter(entry -> matchFileFilter(entry, searchRequest))
                .map(entry -> new ScoredEntry(entry, approximateScore(queryVector, entry.vector())))
                .filter(scoredEntry -> scoredEntry.score() >= searchRequest.getSimilarityThreshold())
                .sorted(Comparator.comparing(ScoredEntry::score).reversed())
                .limit(candidateSize)
                .limit(searchRequest.getTopK())
                .map(scoredEntry -> toScoredDocument(scoredEntry.entry(), scoredEntry.score()))
                .toList();
    }

    @Override
    public void delete(Expression expression) {
        String expressionText = expression == null ? "" : expression.toString();
        entries.removeIf(entry -> MODULE_NAME.equals(asString(entry.metadata().get(METADATA_MODULE)))
                && matchDeleteFilter(entry, expressionText));
        saveIndex();
    }

    @Override
    public List<Document> listDocuments(String fileName) {
        return entries.stream()
                .filter(entry -> MODULE_NAME.equals(asString(entry.metadata().get(METADATA_MODULE))))
                .filter(entry -> !StringUtils.hasText(fileName) || fileName.equals(asString(entry.metadata().get(METADATA_FILE_NAME))))
                .map(entry -> new Document(entry.id(), entry.text(), entry.metadata()))
                .toList();
    }

    private boolean matchDeleteFilter(Entry entry, String expressionText) {
        Object fileName = entry.metadata().get(METADATA_FILE_NAME);
        return !expressionText.contains(METADATA_FILE_NAME) || fileName == null || expressionText.contains(String.valueOf(fileName));
    }

    private Entry toEntry(Document document) {
        String id = StringUtils.hasText(document.getId()) ? document.getId() : UUID.randomUUID().toString();
        return new Entry(id, document.getText(), new LinkedHashMap<>(document.getMetadata()), normalizeIfNeeded(embeddingModel.embed(document)));
    }

    private Document toScoredDocument(Entry entry, double score) {
        Map<String, Object> metadata = new LinkedHashMap<>(entry.metadata());
        metadata.put("vectorStoreType", "FAISS");
        metadata.put("annAlgorithm", properties.getAnn().getAlgorithm().name());
        return Document.builder()
                .id(entry.id())
                .text(entry.text())
                .metadata(metadata)
                .score(score)
                .build();
    }

    private double approximateScore(float[] queryVector, float[] vector) {
        if (properties.getAnn().getAlgorithm() == AnnAlgorithm.PQ) {
            return cosine(quantize(queryVector), quantize(vector));
        }
        return cosine(queryVector, vector);
    }

    private float[] quantize(float[] vector) {
        float[] quantized = new float[vector.length];
        int levels = Math.max(2, 1 << Math.min(12, Math.max(1, properties.getAnn().getPqBits())));
        for (int index = 0; index < vector.length; index++) {
            quantized[index] = Math.round(vector[index] * levels) / (float) levels;
        }
        return quantized;
    }

    private int resolveCandidateSize(int topK) {
        return switch (properties.getAnn().getAlgorithm()) {
            case HNSW -> Math.max(topK, properties.getAnn().getHnswEfSearch());
            case IVF -> Math.max(topK, properties.getAnn().getIvfNprobe() * Math.max(1, topK));
            case PQ -> Math.max(topK, properties.getAnn().getPqSegments() * Math.max(1, topK));
        };
    }

    private boolean matchFileFilter(Entry entry, SearchRequest searchRequest) {
        String filterText = searchRequest.getFilterExpression() == null ? "" : searchRequest.getFilterExpression().toString();
        Object fileName = entry.metadata().get(METADATA_FILE_NAME);
        return !filterText.contains(METADATA_FILE_NAME) || fileName == null || filterText.contains(String.valueOf(fileName));
    }

    private double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0) {
            return 0D;
        }
        int length = Math.min(left.length, right.length);
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int index = 0; index < length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private float[] normalizeIfNeeded(float[] vector) {
        if (!properties.getFaiss().isNormalizeVectors() || vector == null) {
            return vector;
        }
        double norm = 0D;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0D) {
            return vector;
        }
        float[] normalized = new float[vector.length];
        double sqrtNorm = Math.sqrt(norm);
        for (int index = 0; index < vector.length; index++) {
            normalized[index] = (float) (vector[index] / sqrtNorm);
        }
        return normalized;
    }

    private void loadIndex() {
        if (!Files.exists(indexPath)) {
            return;
        }
        try {
            entries.addAll(objectMapper.readValue(indexPath.toFile(), new TypeReference<List<Entry>>() {}));
        } catch (IOException exception) {
            throw VectorStoreException.internalError("读取 FAISS 本地索引失败", exception);
        }
    }

    private void saveIndex() {
        try {
            Path parent = indexPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(indexPath.toFile(), entries);
        } catch (IOException exception) {
            throw VectorStoreException.internalError("保存 FAISS 本地索引失败", exception);
        }
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record Entry(String id, String text, Map<String, Object> metadata, float[] vector) {
    }

    private record ScoredEntry(Entry entry, double score) {
    }
}

