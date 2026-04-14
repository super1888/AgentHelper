package com.spring.ai.common.config.snowflakeid;

import com.spring.ai.common.id.SnowflakeIdGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花主键生成器配置。
 */
@Configuration
@EnableConfigurationProperties(SnowflakeIdProperties.class)
public class SnowflakeIdConfig {

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(SnowflakeIdProperties properties) {
        return new SnowflakeIdGenerator(properties.getWorkerId(), properties.getDatacenterId());
    }
}
