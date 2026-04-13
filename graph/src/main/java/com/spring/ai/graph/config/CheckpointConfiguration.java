package com.spring.ai.graph.config;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 *  checkpoint 基础配置。
 *
 * <p>这个配置类负责把底层 saver 组装好，并对外暴露统一的动态 saver Bean。
 */
@Configuration
@EnableConfigurationProperties(CheckpointProperties.class)
public class CheckpointConfiguration {

    @Bean("approvalWorkflowMemorySaver")
    public MemorySaver approvalWorkflowMemorySaver() {
        return new MemorySaver();
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient approvalWorkflowRedissonClient(CheckpointProperties properties) {
        Config config = new Config();
        CheckpointProperties.Redis redis = properties.getRedis();
        config.useSingleServer()
                .setAddress(redis.getAddress())
                .setDatabase(redis.getDatabase() == null ? 0 : redis.getDatabase());

        if (StringUtils.hasText(redis.getUsername())) {
            config.useSingleServer().setUsername(redis.getUsername());
        }
        if (StringUtils.hasText(redis.getPassword())) {
            config.useSingleServer().setPassword(redis.getPassword());
        }
        return Redisson.create(config);
    }

    @Bean("approvalWorkflowRedisSaver")
    public RedisSaver approvalWorkflowRedisSaver(RedissonClient approvalWorkflowRedissonClient) {
        return RedisSaver.builder()
                .redisson(approvalWorkflowRedissonClient)
                .stateSerializer(StateGraph.DEFAULT_JACKSON_SERIALIZER)
                .build();
    }

    @Bean("approvalWorkflowCheckpointSaver")
    public BaseCheckpointSaver approvalWorkflowCheckpointSaver(
            CheckpointProperties properties,
            MemorySaver approvalWorkflowMemorySaver,
            RedisSaver approvalWorkflowRedisSaver) {
        return new CheckpointDynamicSaver(
                properties,
                approvalWorkflowMemorySaver,
                approvalWorkflowRedisSaver);
    }
}
