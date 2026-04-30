package com.spring.quickstart.config.mybatis.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.spring.ai.common.id.SnowflakeIdGenerator;
import com.spring.quickstart.config.mybatis.generator.SnowflakeIdentifierGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类。
 */
@Configuration
@EnableConfigurationProperties(MybatisPlusProperties.class)
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 拦截器。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /**
     * 配置雪花 ID 生成器。
     */
    @Bean
    public IdentifierGenerator identifierGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        return new SnowflakeIdentifierGenerator(snowflakeIdGenerator);
    }

    /**
     * 配置全局数据库策略。
     */
    @Bean
    public GlobalConfig globalConfig(IdentifierGenerator identifierGenerator) {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setIdentifierGenerator(identifierGenerator);

        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(IdType.ASSIGN_ID);
        globalConfig.setDbConfig(dbConfig);
        return globalConfig;
    }
}
