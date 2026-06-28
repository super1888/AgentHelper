package com.spring.ai.vectorstore.store;

import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_FILE_NAME;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.METADATA_MODULE;
import static com.spring.ai.common.constants.VectorStoreManagerConstants.MODULE_NAME;

import com.spring.ai.vectorstore.exception.VectorStoreException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.util.StringUtils;

/**
 * Spring AI 向量存储网关，适配 Redis 和 Qdrant 等官方 VectorStore 实现。
 */
public class SpringAiVectorStoreGateway implements VectorStoreGateway {

    private final VectorStore vectorStore;
    private final RedisVectorStoreCapabilityChecker redisCapabilityChecker;
    private final RedisDocumentRepository redisDocumentRepository;
    private final boolean redisStore;
    private final List<Document> mirroredDocuments = new CopyOnWriteArrayList<>();

    public SpringAiVectorStoreGateway(
            VectorStore vectorStore,
            RedisVectorStoreCapabilityChecker redisCapabilityChecker,
            RedisDocumentRepository redisDocumentRepository,
            boolean redisStore
    ) {
        this.vectorStore = vectorStore;
        this.redisCapabilityChecker = redisCapabilityChecker;
        this.redisDocumentRepository = redisDocumentRepository;
        this.redisStore = redisStore;
    }

    @Override
    public void ensureReady() {
        if (redisStore) {
            redisCapabilityChecker.ensureReady();
        }
    }

    @Override
    public void add(List<Document> documents) {
        vectorStore.add(documents);
        mirroredDocuments.addAll(documents);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        return vectorStore.similaritySearch(searchRequest);
    }

    @Override
    public void delete(Expression expression) {
        vectorStore.delete(expression);
        mirroredDocuments.clear();
    }

    @Override
    public List<Document> listDocuments(String fileName) {
        if (!redisStore) {
            throw VectorStoreException.badRequest("当前存储后端暂不支持直接枚举切片，请使用检索接口查询内容");
        }
        if (redisDocumentRepository == null) {
            throw VectorStoreException.badRequest("Redis 向量库需要 RedisDocumentRepository Bean");
        }
        return redisDocumentRepository.listDocuments(fileName).stream()
                .filter(document -> MODULE_NAME.equals(asString(document.getMetadata().get(METADATA_MODULE))))
                .filter(document -> !StringUtils.hasText(fileName) || fileName.equals(asString(document.getMetadata().get(METADATA_FILE_NAME))))
                .toList();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}


