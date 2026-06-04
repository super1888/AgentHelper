package com.spring.ai.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 访问统计服务启动入口，只扫描统计模块组件，支持注册到 Nacos。
 */
@SpringBootApplication(scanBasePackages = "com.spring.ai.statistics")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.spring.ai.statistics")
public class StatisticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatisticsServiceApplication.class, args);
    }
}