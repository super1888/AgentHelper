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
 * MyBatis-Plus 配置类
 * 用于配置 MyBatis-Plus 的相关功能，如拦截器、ID生成器等
 */
@Configuration
@EnableConfigurationProperties(MybatisPlusProperties.class)
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 拦截器
     * @param properties MyBatis-Plus 属性配置
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisPlusProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加乐观锁拦截器
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 添加防全表更新与删除拦截器
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        // 如果启用SQL日志，则添加SQL打印拦截器
        if (properties.isEnableSqlLog()) {
            interceptor.addInnerInterceptor(new SqlPrintInterceptor());
        }
        return interceptor;
    }

    /**
     * 配置ID生成器
     * @param snowflakeIdGenerator 雪花ID生成器
     * @return IdentifierGenerator ID生成器实例
     */
    @Bean
    public IdentifierGenerator identifierGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        return new SnowflakeIdentifierGenerator(snowflakeIdGenerator);
    }

    /**
     * 配置全局配置
     * @param identifierGenerator ID生成器
     * @return GlobalConfig 全局配置实例
     */
    @Bean
    public GlobalConfig globalConfig(IdentifierGenerator identifierGenerator) {
        GlobalConfig globalConfig = new GlobalConfig();
        // 设置ID生成器
        globalConfig.setIdentifierGenerator(identifierGenerator);

        // 配置数据库相关设置
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        // 设置ID生成策略为ASSIGN_ID
        dbConfig.setIdType(IdType.ASSIGN_ID);
        globalConfig.setDbConfig(dbConfig);
        return globalConfig;
    }
}
