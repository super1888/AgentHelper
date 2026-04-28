package com.spring.ai.core.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Core 模块 HTTP 客户端配置。
 */
@Configuration
public class CoreHttpClientConfig {

    /**
     * 提供统一的 RestTemplate，用于转发图片代理请求。
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(20).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(120).toMillis());
        requestFactory.setOutputStreaming(false);
        return new RestTemplate(requestFactory);
    }
}
