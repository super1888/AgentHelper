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
 * 本地 FAISS 风格向量存储网关。
 *
 * <p>说明：</p>
 * <p>1. 该类用于提供单机本地向量索引能力，适合开发、演示、离线实验和轻量知识库。</p>
 * <p>2. 写入时会调用 EmbeddingModel 把文本转换成向量，然后保存到内存 entries 中。</p>
 * <p>3. 每次写入或删除后会把 entries 序列化到 JSON 文件，避免应用重启后索引丢失。</p>
 * <p>4. ANN 参数 HNSW、IVF、PQ 在这里作为本地近似检索策略的配置入口，用于控制候选规模或量化近似。</p>
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
        // embeddingModel：用于把文档文本和用户问题转换成向量，是本地向量检索的基础能力。
        this.embeddingModel = embeddingModel;
        // properties：读取 YAML 中 FAISS 索引路径、是否归一化向量、ANN 算法等配置。
        this.properties = properties;
        // objectMapper：用于将本地索引 entries 保存成 JSON，或启动时从 JSON 恢复索引。
        this.objectMapper = objectMapper;
        this.indexPath = Path.of(properties.getFaiss().getIndexPath());
        // 应用启动时尝试加载历史索引文件，保证本地 FAISS 风格存储具备持久化能力。
        loadIndex();
    }

    @Override
    public void ensureReady() {
        // 本地检索必须依赖 EmbeddingModel，没有模型就无法生成写入向量和查询向量。
        if (embeddingModel == null) {
            throw VectorStoreException.badRequest("FAISS 本地存储需要可用的 EmbeddingModel");
        }
    }

    @Override
    public void add(List<Document> documents) {
        // documents：已经切分好的文档列表。每个 Document 的 text 是需要入库的 chunk 内容，metadata 是文件信息和切分信息。
        ensureReady();
        List<Entry> newEntries = documents.stream()
                // 过滤空文本，避免对空字符串生成无意义向量。
                .filter(document -> StringUtils.hasText(document.getText()))
                // toEntry 会为每个 Document 生成向量，并封装成本地索引条目。
                .map(this::toEntry)
                .toList();
        entries.addAll(newEntries);
        // 写入内存后立即保存到磁盘，保证应用异常退出时尽量不丢索引。
        saveIndex();
    }

    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        // searchRequest.query：用户检索问题，会被转换成查询向量。
        // searchRequest.topK：最终返回给上层的命中数量。
        // searchRequest.similarityThreshold：最低相似度阈值，低于该值的候选会被过滤。
        // searchRequest.filterExpression：模块名或文件名过滤条件。
        ensureReady();
        // 先把用户问题转换成查询向量，并根据配置决定是否做向量归一化。
        float[] queryVector = normalizeIfNeeded(embeddingModel.embed(searchRequest.getQuery()));
        // 根据 ANN 算法计算候选规模。候选规模通常大于 topK，便于近似检索保留更多可排序对象。
        int candidateSize = resolveCandidateSize(searchRequest.getTopK());
        return entries.stream()
                // 只检索当前向量模块的数据，避免和其他模块混用同一个本地索引时互相污染。
                .filter(entry -> MODULE_NAME.equals(asString(entry.metadata().get(METADATA_MODULE))))
                // 如果查询里带了文件名过滤，只保留对应文件的索引条目。
                .filter(entry -> matchFileFilter(entry, searchRequest))
                // approximateScore 根据配置选择普通余弦相似度或 PQ 量化后的近似相似度。
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
        // expression：删除过滤表达式。清空模块时只包含模块名；按文件删除时还包含文件名。
        String expressionText = expression == null ? "" : expression.toString();
        entries.removeIf(entry -> MODULE_NAME.equals(asString(entry.metadata().get(METADATA_MODULE)))
                && matchDeleteFilter(entry, expressionText));
        // 删除后立即落盘，保证本地索引文件和内存状态一致。
        saveIndex();
    }

    @Override
    public List<Document> listDocuments(String fileName) {
        // fileName：关键词检索或管理页传入的文件名过滤条件，为空时返回当前模块全部切片。
        return entries.stream()
                .filter(entry -> MODULE_NAME.equals(asString(entry.metadata().get(METADATA_MODULE))))
                .filter(entry -> !StringUtils.hasText(fileName) || fileName.equals(asString(entry.metadata().get(METADATA_FILE_NAME))))
                .map(entry -> new Document(entry.id(), entry.text(), entry.metadata()))
                .toList();
    }

    private boolean matchDeleteFilter(Entry entry, String expressionText) {
        // expressionText 是 Spring AI Filter Expression 的字符串形式，这里用它判断是否包含文件名过滤。
        Object fileName = entry.metadata().get(METADATA_FILE_NAME);
        return !expressionText.contains(METADATA_FILE_NAME) || fileName == null || expressionText.contains(String.valueOf(fileName));
    }

    private Entry toEntry(Document document) {
        // document.id 为空时生成 UUID，保证本地索引条目有稳定唯一标识。
        String id = StringUtils.hasText(document.getId()) ? document.getId() : UUID.randomUUID().toString();
        // embeddingModel.embed(document) 会读取 Document 文本并生成向量，metadata 原样保留用于过滤和展示。
        return new Entry(id, document.getText(), new LinkedHashMap<>(document.getMetadata()), normalizeIfNeeded(embeddingModel.embed(document)));
    }

    private Document toScoredDocument(Entry entry, double score) {
        // 将本地索引条目还原为 Spring AI Document，并补充向量库类型和 ANN 算法，便于调用方观察结果来源。
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
        // PQ 模式下先对向量做量化，再计算余弦相似度，用于模拟乘积量化带来的近似效果。
        if (properties.getAnn().getAlgorithm() == AnnAlgorithm.PQ) {
            return cosine(quantize(queryVector), quantize(vector));
        }
        return cosine(queryVector, vector);
    }

    private float[] quantize(float[] vector) {
        // pqBits：量化位数。位数越大，量化越精细；位数越小，压缩越强但误差更大。
        float[] quantized = new float[vector.length];
        int levels = Math.max(2, 1 << Math.min(12, Math.max(1, properties.getAnn().getPqBits())));
        for (int index = 0; index < vector.length; index++) {
            quantized[index] = Math.round(vector[index] * levels) / (float) levels;
        }
        return quantized;
    }

    private int resolveCandidateSize(int topK) {
        // topK：最终返回数量。ANN 候选数量一般要大于 topK，避免近似检索过早丢弃相关文档。
        return switch (properties.getAnn().getAlgorithm()) {
            // HNSW 使用 efSearch 控制搜索广度，值越大召回越充分但计算越慢。
            case HNSW -> Math.max(topK, properties.getAnn().getHnswEfSearch());
            // IVF 使用 nprobe 控制探测的倒排桶数量，这里用 nprobe * topK 作为候选规模。
            case IVF -> Math.max(topK, properties.getAnn().getIvfNprobe() * Math.max(1, topK));
            // PQ 使用分段数量估算候选规模，给量化近似结果保留更多排序空间。
            case PQ -> Math.max(topK, properties.getAnn().getPqSegments() * Math.max(1, topK));
        };
    }

    private boolean matchFileFilter(Entry entry, SearchRequest searchRequest) {
        // searchRequest.filterExpression 中如果包含文件名条件，则只允许同名文件命中。
        String filterText = searchRequest.getFilterExpression() == null ? "" : searchRequest.getFilterExpression().toString();
        Object fileName = entry.metadata().get(METADATA_FILE_NAME);
        return !filterText.contains(METADATA_FILE_NAME) || fileName == null || filterText.contains(String.valueOf(fileName));
    }

    private double cosine(float[] left, float[] right) {
        // 余弦相似度用于衡量两个向量方向是否接近，值越大表示语义越相似。
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
        // normalizeVectors=true 时将向量缩放为单位向量，让余弦相似度更稳定。
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
        // indexPath：YAML 中 app.vector-store.faiss.index-path 配置的本地索引文件路径。
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
                // 确保索引目录存在，避免首次保存时报目录不存在。
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

