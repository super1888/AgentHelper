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
 * Spring AI 向量存储网关。
 *
 * <p>适配范围：</p>
 * <p>1. Redis：通过 Spring AI RedisVectorStore 完成向量写入、检索和删除。</p>
 * <p>2. Qdrant：通过 Spring AI QdrantVectorStore 完成向量写入、检索和删除。</p>
 *
 * <p>为什么需要该网关：</p>
 * <p>1. 上层服务不应该直接关心当前注入的是 RedisVectorStore 还是 QdrantVectorStore。</p>
 * <p>2. 混合检索里的关键词检索需要枚举文档；Redis 可以扫描 RedisJSON，Qdrant 默认不做全量枚举，所以这里为非 Redis 后端维护运行期镜像。</p>
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
        // vectorStore：Spring AI 自动配置出来的真实向量库对象，可能是 RedisVectorStore 或 QdrantVectorStore。
        this.vectorStore = vectorStore;
        // redisCapabilityChecker：Redis 模式专用，用于启动前检查 Redis 模块能力是否满足向量库要求。
        this.redisCapabilityChecker = redisCapabilityChecker;
        // redisDocumentRepository：Redis 模式专用，用于扫描 RedisJSON 文档，给关键词检索提供候选文本。
        this.redisDocumentRepository = redisDocumentRepository;
        // redisStore：标识当前是否为 Redis 后端。true 时使用 Redis 枚举能力，false 时使用运行期镜像。
        this.redisStore = redisStore;
    }

    @Override
    public void ensureReady() {
        if (redisStore) {
            // Redis 向量检索依赖 RedisJSON 和 RediSearch，如果能力缺失，提前抛出清晰错误。
            redisCapabilityChecker.ensureReady();
        }
    }

    @Override
    public void add(List<Document> documents) {
        // documents：已经切分好的文档列表。Spring AI VectorStore 会负责调用 EmbeddingModel 并写入后端。
        vectorStore.add(documents);
        // 非 Redis 后端没有直接枚举实现时，运行期镜像用于关键词检索。Redis 模式保留该镜像也不影响主流程。
        mirroredDocuments.addAll(documents);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        // searchRequest：包含 query、topK、similarityThreshold、filterExpression，直接透传给 Spring AI VectorStore。
        return vectorStore.similaritySearch(searchRequest);
    }

    @Override
    public void delete(Expression expression) {
        // expression：删除过滤条件。Spring AI 会根据表达式删除匹配的向量文档。
        vectorStore.delete(expression);
        // 删除后清空运行期镜像，避免关键词检索命中过期数据。
        mirroredDocuments.clear();
    }

    @Override
    public List<Document> listDocuments(String fileName) {
        if (!redisStore) {
            // Qdrant 等后端默认使用运行期镜像参与关键词检索。应用重启后镜像为空时，关键词检索会自然降级为空结果。
            return mirroredDocuments.stream()
                    .filter(document -> MODULE_NAME.equals(asString(document.getMetadata().get(METADATA_MODULE))))
                    .filter(document -> !StringUtils.hasText(fileName) || fileName.equals(asString(document.getMetadata().get(METADATA_FILE_NAME))))
                    .toList();
        }
        if (redisDocumentRepository == null) {
            throw VectorStoreException.badRequest("Redis 向量库需要 RedisDocumentRepository Bean");
        }
        // Redis 模式通过仓储扫描真实 Redis 数据，适合服务重启后继续做关键词检索。
        return redisDocumentRepository.listDocuments(fileName).stream()
                .filter(document -> MODULE_NAME.equals(asString(document.getMetadata().get(METADATA_MODULE))))
                .filter(document -> !StringUtils.hasText(fileName) || fileName.equals(asString(document.getMetadata().get(METADATA_FILE_NAME))))
                .toList();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}


