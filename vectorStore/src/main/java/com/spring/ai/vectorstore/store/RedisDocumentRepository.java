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
 * Redis 向量库文档仓储，用于管理页枚举切片和本地关键词检索。
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
     * @param fileName 文件名过滤，可为空
     * @return 文档列表
     */
    public List<Document> listDocuments(String fileName) {
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
        VectorStore vectorStore = vectorStoreProvider.stream()
                .filter(candidate -> candidate.getClass().getName().toLowerCase().contains("redis"))
                .findFirst()
                .orElse(null);
        if (vectorStore == null) {
            throw new VectorStoreException(HttpStatus.SERVICE_UNAVAILABLE, "当前存储后端未提供 Redis VectorStore Bean");
        }
        return vectorStore.getNativeClient()
                .filter(JedisPooled.class::isInstance)
                .map(JedisPooled.class::cast)
                .orElseThrow(() -> new VectorStoreException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "当前向量存储未提供 Jedis 客户端"));
    }

    private List<String> scanVectorKeys(JedisPooled jedis) {
        List<String> keys = new ArrayList<>();
        String cursor = ScanParams.SCAN_POINTER_START;
        ScanParams scanParams = new ScanParams()
                .match(redisVectorPrefix + "*")
                .count(FILE_LIST_SCAN_COUNT);
        do {
            ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
            keys.addAll(scanResult.getResult());
            cursor = scanResult.getCursor();
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
        return keys;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readStoredDocument(JedisPooled jedis, String key) {
        Object jsonObject = jedis.jsonGet(key);
        if (jsonObject instanceof Map<?, ?> jsonMap) {
            return (Map<String, Object>) jsonMap;
        }
        return Map.of();
    }

    private Document toDocument(Map<String, Object> documentMap) {
        Object content = documentMap.getOrDefault("content", documentMap.get("text"));
        return Document.builder()
                .id(asString(documentMap.get("id")))
                .text(asString(content))
                .metadata(resolveMetadata(documentMap))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveMetadata(Map<String, Object> documentMap) {
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


