package com.spring.quickstart.config.mybatis.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.spring.ai.common.id.SnowflakeIdGenerator;
import com.spring.quickstart.config.mybatis.generator.SnowflakeIdentifierGenerator;
import com.spring.quickstart.config.mybatis.interceptor.SqlPrintInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 通用配置。 统一在 common 模块配置主键生成和安全类插件。
 */
@Configuration
@EnableConfigurationProperties(MybatisPlusProperties.class)
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 主拦截器。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisPlusProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 是否开启乐观锁插件
        if (properties.isEnableOptimisticLocker()) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        }
        // 攻击拦截器
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        // 是否开启 SQL 打印
        if (properties.isEnableSqlLog()) {
            interceptor.addInnerInterceptor(new SqlPrintInterceptor());
        }
        return interceptor;
    }

    /**
     * 统一主键生成器。
     */
    @Bean
    public IdentifierGenerator identifierGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        return new SnowflakeIdentifierGenerator(snowflakeIdGenerator);
    }

    /**
     * MyBatis-Plus 全局配置。
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
