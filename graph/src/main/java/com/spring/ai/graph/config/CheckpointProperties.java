package com.spring.ai.graph.config;

import com.spring.ai.common.enums.graph.ApprovalWorkflowCheckpointModeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * checkpoint 配置。
 *
 * <p>这里不把配置写死在代码里，而是通过配置中心或本地配置文件切换：
 * `app.workflow.checkpoint.type=MEMORY|REDIS`
 *
 * <p>如果选择 REDIS，则使用内嵌的 redis 配置构造 RedissonClient。
 */
@ConfigurationProperties(prefix = "app.checkpoint")
@Data
public class CheckpointProperties {

    /**
     * 默认用内存，保证开箱即用。
     */
    private ApprovalWorkflowCheckpointModeEnum type = ApprovalWorkflowCheckpointModeEnum.MEMORY;

    private Redis redis = new Redis();

    @Data
    public static class Redis {

        /**
         * Redis 地址，Redisson 单机模式要求带 redis:// 前缀。
         */
        private String address = "redis://127.0.0.1:6379";

        private String username;

        private String password;

        private Integer database = 0;

    }
}
