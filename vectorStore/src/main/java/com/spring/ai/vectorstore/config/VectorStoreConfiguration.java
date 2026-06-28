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
 * 向量存储模块配置，负责文本格式化、切片器和多存储后端网关装配。
 */
@Configuration
@EnableConfigurationProperties(VectorStoreProperties.class)
public class VectorStoreConfiguration {

    /**
     * 显式提供 Jedis 连接工厂，满足 Spring AI RedisVectorStore 自动配置条件。
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

