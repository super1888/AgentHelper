package com.spring.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * quickStart 启动类。
 *
 * <p>该模块既承担项目启动入口，也承担学习和演示用途，因此需要同时扫描
 * `com.spring.ai` 和 `com.spring.quickstart` 两个包，避免本模块内的配置类遗漏。</p>
 */
@SpringBootApplication
@ComponentScan({"com.spring.ai", "com.spring.quickstart"})
public class QuickStartApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickStartApplication.class, args);
    }
}
