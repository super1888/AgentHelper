package com.spring.quickstart.config.mybatis.config;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataChangeRecorderInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.IllegalSQLInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.spring.ai.common.id.SnowflakeIdGenerator;
import com.spring.quickstart.config.mybatis.generator.SnowflakeIdentifierGenerator;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 通用配置。
 * 统一在 common 模块配置主键生成、逻辑删除和安全类插件。
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
        if (properties.isEnableOptimisticLocker()) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        }

        DataChangeRecorderInnerInterceptor dataChangeRecorder = new DataChangeRecorderInnerInterceptor();
        dataChangeRecorder.setBatchUpdateLimit(1000);
        dataChangeRecorder.openBatchUpdateLimitation();
        interceptor.addInnerInterceptor(dataChangeRecorder);

        interceptor.addInnerInterceptor(new IllegalSQLInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
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
     * 统一逻辑删除取值：未删除 0，删除 1。
     */
    @Bean
    public GlobalConfig globalConfig(IdentifierGenerator identifierGenerator) {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setIdentifierGenerator(identifierGenerator);

        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(IdType.ASSIGN_ID);
        dbConfig.setLogicDeleteField("deleteFlag");
        dbConfig.setLogicNotDeleteValue("0");
        dbConfig.setLogicDeleteValue("1");
        globalConfig.setDbConfig(dbConfig);
        return globalConfig;
    }

    /**
     * 按配置决定是否打印 SQL。
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer(MybatisPlusProperties properties) {
        return configuration -> {
            if (properties.isEnableSqlLog()) {
                configuration.setLogImpl(StdOutImpl.class);
            }
        };
    }
}
