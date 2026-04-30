package com.spring.ai.logging.config;

import com.spring.ai.logging.web.RequestTraceLogFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 日志模块自动装配。
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentHelperLoggingProperties.class)
public class AgentHelperLoggingAutoConfiguration {

    /**
     * 注册请求链路追踪过滤器。
     */
    @Bean
    @ConditionalOnMissingBean
    public RequestTraceLogFilter requestTraceLogFilter(AgentHelperLoggingProperties loggingProperties) {
        return new RequestTraceLogFilter(loggingProperties);
    }
}
