package com.spring.ai.logging.config;

import com.spring.ai.logging.mybatis.MybatisSqlLogInterceptor;
import com.spring.ai.logging.web.RequestTraceLogFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 日志模块自动装配。
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentHelperLoggingProperties.class)
public class AgentHelperLoggingAutoConfiguration {

    /**
     * 注册 traceId 与访问日志过滤器。
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestTraceLogFilter requestTraceLogFilter(AgentHelperLoggingProperties loggingProperties) {
        return new RequestTraceLogFilter(loggingProperties);
    }

    /**
     * 注册 SQL 日志拦截器。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.apache.ibatis.plugin.Interceptor")
    @ConditionalOnProperty(prefix = "app.mybatis-plus", name = "enable-sql-log", havingValue = "true")
    public MybatisSqlLogInterceptor mybatisSqlLogInterceptor(AgentHelperLoggingProperties loggingProperties) {
        return new MybatisSqlLogInterceptor(loggingProperties);
    }
}
