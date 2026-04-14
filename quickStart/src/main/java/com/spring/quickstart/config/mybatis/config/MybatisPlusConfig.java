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

@Configuration
@EnableConfigurationProperties(MybatisPlusProperties.class)
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisPlusProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        if (properties.isEnableOptimisticLocker()) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        }
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        if (properties.isEnableSqlLog()) {
            interceptor.addInnerInterceptor(new SqlPrintInterceptor());
        }
        return interceptor;
    }

    @Bean
    public IdentifierGenerator identifierGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        return new SnowflakeIdentifierGenerator(snowflakeIdGenerator);
    }

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
