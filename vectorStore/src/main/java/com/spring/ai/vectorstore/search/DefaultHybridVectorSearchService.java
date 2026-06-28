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
 * 默认混合检索服务。
 *
 * <p>完整处理流程：</p>
 * <p>1. 读取 YAML 中的 app.vector-store.hybrid-search 配置，判断是否启用混合检索。</p>
 * <p>2. 如果启用向量检索，则先使用用户问题做语义向量召回，适合找到“表达不同但含义接近”的内容。</p>
 * <p>3. 如果启用关键词检索，则枚举候选文档并计算词项命中分数，适合找到“关键词完全命中”的内容。</p>
 * <p>4. 将两路候选结果交给 RRF 算法融合，避免单一路径召回结果过于偏置。</p>
 * <p>5. 如果启用 Rerank，则对融合后的候选列表再做一次重排，提升最终 topK 的相关性。</p>
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
        // searchRequest：包含用户问题、topK、相似度阈值和过滤条件，是混合检索的统一入口参数。
        // fileName：文件名过滤条件，关键词检索枚举文档时会使用；为空表示不按文件过滤。
        VectorStoreProperties.HybridSearch hybrid = properties.getHybridSearch();
        if (!hybrid.isEnabled()) {
            // 未启用混合检索时，直接走向量检索，保持与 Spring AI 默认 VectorStore 行为一致。
            return vectorStoreGateway.similaritySearch(searchRequest);
        }

        List<List<Document>> rankedLists = new ArrayList<>();
        if (hybrid.isVectorEnabled()) {
            // 第一路召回：向量检索。返回的是按语义相似度排序的候选列表。
            rankedLists.add(vectorSearch(searchRequest, hybrid));
        }
        if (hybrid.isKeywordEnabled()) {
            // 第二路召回：关键词检索。返回的是按词项命中比例排序的候选列表。
            List<Document> keywordDocuments = keywordSearch(searchRequest.getQuery(), fileName, searchRequest.getTopK(), hybrid);
            if (!keywordDocuments.isEmpty()) {
                rankedLists.add(keywordDocuments);
            }
        }
        // RRF 融合：把向量检索和关键词检索的排序名次转换成统一分数，再合并去重。
        List<Document> fusedDocuments = reciprocalRankFusion(rankedLists, hybrid.getRrfK(), searchRequest.getTopK(), hybrid);
        if (hybrid.isRerankEnabled()) {
            // Rerank 重排：对融合后的候选集做二次排序，最终只返回 topK 条给 RAG 上下文。
            fusedDocuments = documentReranker.rerank(searchRequest.getQuery(), fusedDocuments);
        }
        return fusedDocuments.stream().limit(searchRequest.getTopK()).toList();
    }

    private List<Document> vectorSearch(SearchRequest searchRequest, VectorStoreProperties.HybridSearch hybrid) {
        // vectorCandidateMultiplier：向量召回放大倍数。先多召回一些候选，给后续 RRF 和 Rerank 留出选择空间。
        int candidateTopK = Math.max(searchRequest.getTopK(), searchRequest.getTopK() * Math.max(1, hybrid.getVectorCandidateMultiplier()));
        // 复用原始 query、similarityThreshold、filterExpression，只扩大 topK，不改变用户问题和过滤条件。
        SearchRequest.Builder builder = SearchRequest.from(searchRequest).topK(candidateTopK);
        return vectorStoreGateway.similaritySearch(builder.build());
    }

    private List<Document> keywordSearch(
            String query,
            String fileName,
            int topK,
            VectorStoreProperties.HybridSearch hybrid
    ) {
        // query：用户原始问题，用于拆分关键词。
        // fileName：可选文件过滤条件，用于限制关键词检索只在某个文件的切片中进行。
        // topK：最终希望返回给上层的数量，关键词检索会按倍率扩大候选数量。
        Set<String> queryTerms = tokenize(query);
        // keywordCandidateMultiplier：关键词召回放大倍数。关键词检索先取更多候选，再交给 RRF 融合。
        int candidateSize = Math.max(topK, topK * Math.max(1, hybrid.getKeywordCandidateMultiplier()));
        try {
            // listDocuments 会枚举可检索文本。Redis 从 RedisJSON 扫描，FAISS 从本地索引枚举，Qdrant 使用运行期镜像。
            return vectorStoreGateway.listDocuments(fileName).stream()
                    // 为每个文档计算关键词分数，并把分数写入 Document.score 和 metadata.keywordScore。
                    .map(document -> withScore(document, keywordScore(queryTerms, document)))
                    // 过滤掉没有任何关键词命中的文档，避免无关文本进入融合阶段。
                    .filter(document -> document.getScore() != null && document.getScore() > 0D)
                    .sorted(Comparator.comparing(this::documentScore).reversed())
                    .limit(candidateSize)
                    .toList();
        } catch (RuntimeException exception) {
            // 某些后端无法枚举文档时，关键词检索自动降级为空列表，整体检索仍可依赖向量召回继续工作。
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
                // documentKey 用于跨检索路径去重。同一个文档既被向量命中又被关键词命中时，会累加 RRF 分数。
                String key = documentKey(document);
                FusionItem fusionItem = fusionItems.computeIfAbsent(key, ignored -> new FusionItem(document, 0D));
                // RRF 公式：score += 1 / (rrfK + rank)。rank 越靠前，贡献越大；rrfK 越大，不同名次之间差距越平滑。
                fusionItem.score += 1D / (Math.max(1, rrfK) + index + 1D);
            }
        }
        // rerankCandidateMultiplier：进入重排阶段的候选放大倍数。避免过早截断导致优质候选丢失。
        int rerankCandidateSize = Math.max(topK, topK * Math.max(1, hybrid.getRerankCandidateMultiplier()));
        return fusionItems.values().stream()
                // 将融合分数写入 Document.score 和 metadata.rrfScore，便于测试和接口输出观察。
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
        // 命中分数 = 查询词中有多少比例出现在文档中。该分数简单、可解释，适合做关键词召回的初筛。
        long hitCount = queryTerms.stream().filter(documentTerms::contains).count();
        return hitCount / (double) queryTerms.size();
    }

    private Document withScore(Document document, double score) {
        // 复制原始元数据，避免直接修改原 Document 的 metadata，减少副作用。
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
        // RRF 分数写入元数据后，调用方可以区分“原始向量分数”和“融合后的排序分数”。
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
        // 优先使用 Document.id 做稳定去重；如果没有 id，则退化使用文本内容去重。
        return StringUtils.hasText(document.getId()) ? document.getId() : document.getText();
    }

    private Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        // 按非中文、非字母、非数字字符切分，兼容中文关键词和英文代码标识符。
        return new HashSet<>(Arrays.stream(text.toLowerCase().split("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}]+"))
                .filter(StringUtils::hasText)
                .toList());
    }

    private static class FusionItem {
        // document：融合后的原始文档引用；score：该文档在多路召回中的累计 RRF 分数。
        private final Document document;
        private double score;

        private FusionItem(Document document, double score) {
            this.document = document;
            this.score = score;
        }
    }
}


