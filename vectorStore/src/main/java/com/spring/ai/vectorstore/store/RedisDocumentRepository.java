package com.spring.ai.vectorstore.store;

import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_FILE_NAME;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_MODULE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.MODULE_NAME;

import com.spring.ai.vectorstore.exception.VectorStoreException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * Redis 向量库文档仓储。
 *
 * <p>核心作用：</p>
 * <p>1. Spring AI RedisVectorStore 写入的数据保存在 RedisJSON 中。</p>
 * <p>2. 向量检索可以直接调用 VectorStore.similaritySearch，但关键词检索需要先拿到文本内容。</p>
 * <p>3. 该仓储负责扫描 Redis 中的向量文档 key，读取 JSON 内容，并还原成 Spring AI Document。</p>
 */
@Repository
public class RedisDocumentRepository {

    private static final int FILE_LIST_SCAN_COUNT = 200;

    private final ObjectProvider<VectorStore> vectorStoreProvider;

    @Value("${spring.ai.vectorstore.redis.prefix:vector:}")
    private String redisVectorPrefix;

    public RedisDocumentRepository(ObjectProvider<VectorStore> vectorStoreProvider) {
        this.vectorStoreProvider = vectorStoreProvider;
    }

    /**
     * 列出 Redis 中当前模块的文档。
     *
     * @param fileName 文件名过滤条件。为空时返回当前模块全部文档；非空时只返回指定文件的切片。
     * @return Redis 中还原出来的文档切片列表。
     *
     * <p>处理步骤：</p>
     * <p>1. 获取 Spring AI RedisVectorStore 暴露的 JedisPooled 原生客户端。</p>
     * <p>2. 按 spring.ai.vectorstore.redis.prefix 扫描所有向量文档 key。</p>
     * <p>3. 对每个 key 执行 JSON.GET，读取文档内容和元数据。</p>
     * <p>4. 过滤模块名，避免读取到其他模块的数据。</p>
     * <p>5. 如果传入 fileName，则继续按文件名过滤。</p>
     * <p>6. 将 Redis JSON Map 还原为 Document，供关键词检索或管理接口使用。</p>
     */
    public List<Document> listDocuments(String fileName) {
        // jedis：Redis 原生客户端，用于执行 SCAN 和 JSON.GET。
        JedisPooled jedis = resolveJedisClient();
        return scanVectorKeys(jedis).stream()
                .map(key -> readStoredDocument(jedis, key))
                .filter(documentMap -> MODULE_NAME.equals(asString(resolveMetadata(documentMap).get(METADATA_MODULE))))
                .filter(documentMap -> !StringUtils.hasText(fileName)
                        || fileName.equals(asString(resolveMetadata(documentMap).get(METADATA_FILE_NAME))))
                .map(this::toDocument)
                .toList();
    }

    private JedisPooled resolveJedisClient() {
        // vectorStoreProvider：容器中可能同时存在 Redis 和 Qdrant VectorStore，这里明确选择类名包含 redis 的实现。
        VectorStore vectorStore = vectorStoreProvider.stream()
                .filter(candidate -> candidate.getClass().getName().toLowerCase().contains("redis"))
                .findFirst()
                .orElse(null);
        if (vectorStore == null) {
            throw new VectorStoreException(HttpStatus.SERVICE_UNAVAILABLE, "当前存储后端未提供 Redis VectorStore Bean");
        }
        // getNativeClient：Spring AI RedisVectorStore 暴露底层客户端，只有 Redis 模式才应该返回 JedisPooled。
        return vectorStore.getNativeClient()
                .filter(JedisPooled.class::isInstance)
                .map(JedisPooled.class::cast)
                .orElseThrow(() -> new VectorStoreException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "当前向量存储未提供 Jedis 客户端"));
    }

    private List<String> scanVectorKeys(JedisPooled jedis) {
        // keys：收集扫描到的 Redis key。使用 SCAN 而不是 KEYS，避免大量数据时阻塞 Redis。
        List<String> keys = new ArrayList<>();
        String cursor = ScanParams.SCAN_POINTER_START;
        // redisVectorPrefix：Spring AI RedisVectorStore 写入向量文档时使用的 key 前缀。
        ScanParams scanParams = new ScanParams()
                .match(redisVectorPrefix + "*")
                .count(FILE_LIST_SCAN_COUNT);
        do {
            // cursor：Redis SCAN 游标。返回 0 表示本轮扫描完成。
            ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
            keys.addAll(scanResult.getResult());
            cursor = scanResult.getCursor();
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
        return keys;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readStoredDocument(JedisPooled jedis, String key) {
        // key：Redis 中单个向量文档的 key。JSON.GET 返回值通常是 Map 结构。
        Object jsonObject = jedis.jsonGet(key);
        if (jsonObject instanceof Map<?, ?> jsonMap) {
            return (Map<String, Object>) jsonMap;
        }
        return Map.of();
    }

    private Document toDocument(Map<String, Object> documentMap) {
        // Redis JSON 里不同版本可能使用 content 或 text 字段保存正文，这里两者兼容读取。
        Object content = documentMap.getOrDefault("content", documentMap.get("text"));
        return Document.builder()
                .id(asString(documentMap.get("id")))
                .text(asString(content))
                .metadata(resolveMetadata(documentMap))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveMetadata(Map<String, Object> documentMap) {
        // 优先读取标准 metadata 字段；如果不存在，则从扁平字段中兜底提取 metadata.*、fileName、module 等信息。
        Object metadata = documentMap.get("metadata");
        if (metadata instanceof Map<?, ?> metadataMap) {
            return (Map<String, Object>) metadataMap;
        }
        Map<String, Object> fallbackMetadata = new LinkedHashMap<>();
        documentMap.forEach((key, value) -> {
            if (key.startsWith("metadata.") || METADATA_FILE_NAME.equals(key) || METADATA_MODULE.equals(key)) {
                fallbackMetadata.put(key, value);
            }
        });
        return fallbackMetadata;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}


