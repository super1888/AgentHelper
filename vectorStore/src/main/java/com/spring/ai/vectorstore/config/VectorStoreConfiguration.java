package com.spring.ai.vectorstore.config;

import java.time.Duration;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
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
 * <p>除了文本格式化器和切片器外，这里额外显式提供 `JedisConnectionFactory`。
 *
 * <p>原因是 Spring AI 1.1.2 的 `RedisVectorStoreAutoConfiguration` 依赖的是
 * `JedisConnectionFactory`，而不是通用的 `RedisConnectionFactory`。
 * 仅使用 Spring Boot 默认的 Lettuce 连接工厂时，`VectorStore` 自动配置不会生效，
 * 最终表现为容器里缺少 `org.springframework.ai.vectorstore.VectorStore` Bean。</p>
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
}
