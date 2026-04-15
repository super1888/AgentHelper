package com.spring.quickstart.config.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 统一接口前缀配置。
 *
 * <p>所有 RestController 默认追加 `/agentHelper` 前缀，
 * 避免每个控制器重复声明公共前缀。</p>
 */
@Configuration
public class ApiPrefixWebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/agentHelper", HandlerTypePredicate.forAnnotation(RestController.class));
    }
}
