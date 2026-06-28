package com.spring.ai.vectorstore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.vectorstore.config.VectorStoreProperties.StoreType;
import com.spring.ai.vectorstore.store.LocalFaissVectorStoreGateway;
import com.spring.ai.vectorstore.store.RedisDocumentRepository;
import com.spring.ai.vectorstore.store.RedisVectorStoreCapabilityChecker;
import com.spring.ai.vectorstore.store.SpringAiVectorStoreGateway;
import com.spring.ai.vectorstore.store.VectorStoreGateway;
import java.time.Duration;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.StringUtils;

/**
 * 向量存储模块配置。
 *
 * <p>核心职责：</p>
 * <p>1. 读取 app.vector-store 下的业务配置，决定使用 Redis、Qdrant 还是 FAISS。</p>
 * <p>2. 创建 Redis 模式需要的 JedisConnectionFactory，满足 Spring AI RedisVectorStore 的自动配置条件。</p>
 * <p>3. 创建文本格式化器和 Token 切分器，供文件 reader 和切分服务复用。</p>
 * <p>4. 根据 storeType 装配统一的 VectorStoreGateway，让业务服务不直接依赖具体向量库实现。</p>
 */
@Configuration
@EnableConfigurationProperties(VectorStoreProperties.class)
public class VectorStoreConfiguration {

    /**
     * 显式提供 Jedis 连接工厂，满足 Spring AI RedisVectorStore 自动配置条件。
     *
     * @param redisProperties Spring Boot 读取 spring.data.redis 后生成的 Redis 连接配置，包含 host、port、database、username、password、timeout 等信息。
     * @return JedisConnectionFactory，供 Spring AI RedisVectorStore 创建底层 Redis 客户端。
     *
     * <p>处理步骤：</p>
     * <p>1. 将 Spring Boot 的 RedisProperties 转成 RedisStandaloneConfiguration。</p>
     * <p>2. 如果配置了用户名和密码，则写入认证信息。</p>
     * <p>3. 如果配置了连接超时，则同时设置连接超时和读取超时。</p>
     * <p>4. 返回 JedisConnectionFactory，并用 @Primary 避免和默认 Lettuce 工厂冲突。</p>
     */
    @Bean
    @Primary
    public JedisConnectionFactory jedisConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
        standaloneConfiguration.setHostName(redisProperties.getHost());
        standaloneConfiguration.setPort(redisProperties.getPort());
        standaloneConfiguration.setDatabase(redisProperties.getDatabase());

        if (StringUtils.hasText(redisProperties.getUsername())) {
            standaloneConfiguration.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.hasText(redisProperties.getPassword())) {
            standaloneConfiguration.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        JedisClientConfiguration.JedisClientConfigurationBuilder clientConfigurationBuilder =
                JedisClientConfiguration.builder();
        Duration timeout = redisProperties.getTimeout();
        if (timeout != null) {
            clientConfigurationBuilder.connectTimeout(timeout);
            clientConfigurationBuilder.readTimeout(timeout);
        }

        return new JedisConnectionFactory(standaloneConfiguration, clientConfigurationBuilder.build());
    }

    /**
     * 多后端向量库网关。
     *
     * @param properties 向量库业务配置，主要读取 storeType、FAISS 索引路径、ANN 配置等。
     * @param objectProviderBridge 可选 Bean 解析桥，按需获取 VectorStore、EmbeddingModel、Redis 检查器等对象。
     * @param objectMapper JSON 工具，FAISS 本地模式用它保存和读取索引文件。
     * @return 统一向量存储网关。REDIS/QDRANT 返回 SpringAiVectorStoreGateway，FAISS 返回 LocalFaissVectorStoreGateway。
     *
     * <p>处理步骤：</p>
     * <p>1. 如果 storeType=FAISS，说明使用本地索引，不依赖 Spring AI VectorStore Bean。</p>
     * <p>2. 如果 storeType=REDIS 或 QDRANT，则从容器中选择对应类型的 Spring AI VectorStore。</p>
     * <p>3. 将具体后端包装成 VectorStoreGateway，供 VectorStoreServiceImpl、混合检索服务统一调用。</p>
     */
    @Bean
    @ConditionalOnMissingBean(VectorStoreGateway.class)
    public VectorStoreGateway vectorStoreGateway(
            VectorStoreProperties properties,
            ObjectProviderBridge objectProviderBridge,
            ObjectMapper objectMapper
    ) {
        if (properties.getStoreType() == StoreType.FAISS) {
            return new LocalFaissVectorStoreGateway(objectProviderBridge.embeddingModel(), properties, objectMapper);
        }
        VectorStore vectorStore = objectProviderBridge.vectorStore(properties.getStoreType());
        return new SpringAiVectorStoreGateway(
                vectorStore,
                objectProviderBridge.redisVectorStoreCapabilityChecker(),
                objectProviderBridge.redisDocumentRepository(),
                properties.getStoreType() == StoreType.REDIS);
    }

    /**
     * 延迟解析可选 Bean，避免 FAISS 模式强依赖 Redis VectorStore。
     *
     * <p>处理含义：</p>
     * <p>1. Redis 和 Qdrant starter 同时存在时，容器里可能出现多个 VectorStore Bean。</p>
     * <p>2. FAISS 模式不需要 VectorStore，但需要 EmbeddingModel。</p>
     * <p>3. 使用 ObjectProvider 延迟获取 Bean，可以按当前 storeType 精确选择需要的 Bean。</p>
     */
    @Bean
    public ObjectProviderBridge objectProviderBridge(
            org.springframework.beans.factory.ObjectProvider<VectorStore> vectorStoreProvider,
            org.springframework.beans.factory.ObjectProvider<EmbeddingModel> embeddingModelProvider,
            org.springframework.beans.factory.ObjectProvider<RedisVectorStoreCapabilityChecker> capabilityCheckerProvider,
            org.springframework.beans.factory.ObjectProvider<RedisDocumentRepository> redisDocumentRepositoryProvider
    ) {
        return new ObjectProviderBridge(
                vectorStoreProvider,
                embeddingModelProvider,
                capabilityCheckerProvider,
                redisDocumentRepositoryProvider);
    }

    /**
     * 文本格式化器。
     *
     * @param properties 向量库配置，读取 PDF 文本抽取时的页眉页脚清理、左对齐等格式化参数。
     * @return ExtractedTextFormatter，供 PDF reader 等组件在抽取文本时清理无效行。
     */
    @Bean
    public ExtractedTextFormatter extractedTextFormatter(VectorStoreProperties properties) {
        return ExtractedTextFormatter.builder()
                .withLeftAlignment(properties.isLeftAlignment())
                .withNumberOfTopPagesToSkipBeforeDelete(properties.getNumberOfTopPagesToSkipBeforeDelete())
                .withNumberOfTopTextLinesToDelete(properties.getNumberOfTopTextLinesToDelete())
                .withNumberOfBottomTextLinesToDelete(properties.getNumberOfBottomTextLinesToDelete())
                .build();
    }

    /**
     * Token 级文本切片器。
     *
     * @param properties 向量库配置，读取 chunkSize、minChunkSizeChars、minChunkLengthToEmbed、maxNumChunks、keepSeparator 等切分参数。
     * @return TokenTextSplitter，作为 AUTO 模式或自适应兜底切分器使用。
     *
     * <p>参数含义：</p>
     * <p>1. chunkSize：目标切片大小。</p>
     * <p>2. minChunkSizeChars：最小切片字符数，避免生成过碎文本。</p>
     * <p>3. minChunkLengthToEmbed：低于该长度的切片不适合做嵌入。</p>
     * <p>4. maxNumChunks：单个文档最多切片数量，避免异常大文件导致过多请求。</p>
     * <p>5. keepSeparator：是否保留分隔符，保留分隔符通常有助于维持语义边界。</p>
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter(VectorStoreProperties properties) {
        return TokenTextSplitter.builder()
                .withChunkSize(properties.getChunkSize())
                .withMinChunkSizeChars(properties.getMinChunkSizeChars())
                .withMinChunkLengthToEmbed(properties.getMinChunkLengthToEmbed())
                .withMaxNumChunks(properties.getMaxNumChunks())
                .withKeepSeparator(properties.isKeepSeparator())
                .build();
    }

    /**
     * 可选 Bean 解析桥接器。
     *
     * @param vectorStoreProvider 容器中的 Spring AI VectorStore 候选对象，可能包含 RedisVectorStore 或 QdrantVectorStore。
     * @param embeddingModelProvider 容器中的 EmbeddingModel，用于 FAISS 本地向量生成。
     * @param capabilityCheckerProvider Redis 能力检查器，用于 Redis 模式启动前检查 RedisJSON 和 RediSearch。
     * @param redisDocumentRepositoryProvider Redis 文档仓储，用于关键词检索枚举 Redis 文档。
     */
    public record ObjectProviderBridge(
            org.springframework.beans.factory.ObjectProvider<VectorStore> vectorStoreProvider,
            org.springframework.beans.factory.ObjectProvider<EmbeddingModel> embeddingModelProvider,
            org.springframework.beans.factory.ObjectProvider<RedisVectorStoreCapabilityChecker> capabilityCheckerProvider,
            org.springframework.beans.factory.ObjectProvider<RedisDocumentRepository> redisDocumentRepositoryProvider
    ) {
        public VectorStore vectorStore(StoreType storeType) {
            String expectedName = storeType == StoreType.QDRANT ? "qdrant" : "redis";
            return vectorStoreProvider.stream()
                    .filter(vectorStore -> vectorStore.getClass().getName().toLowerCase().contains(expectedName))
                    .findFirst()
                    .orElseGet(() -> java.util.Objects.requireNonNull(
                            vectorStoreProvider.getIfAvailable(),
                            "当前存储后端需要 VectorStore Bean，请检查对应 starter 和 YAML 配置"));
        }

        public EmbeddingModel embeddingModel() {
            return java.util.Objects.requireNonNull(embeddingModelProvider.getIfAvailable(), "FAISS 本地存储需要 EmbeddingModel Bean，请检查嵌入模型配置");
        }

        public RedisVectorStoreCapabilityChecker redisVectorStoreCapabilityChecker() {
            return capabilityCheckerProvider.getIfAvailable();
        }

        public RedisDocumentRepository redisDocumentRepository() {
            return redisDocumentRepositoryProvider.getIfAvailable();
        }
    }
}

