package com.spring.ai.common.config.async;

import com.spring.ai.common.web.WebSocketUserContextHolder;
import java.util.concurrent.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 公共线程池配置。
 * 通过@Configuration注解标记为配置类，并启用CommonAsyncProperties配置属性。
 */
@Configuration
@EnableConfigurationProperties(CommonAsyncProperties.class)
public class CommonAsyncConfig {

    // 定义公共异步执行器的Bean名称常量
    public static final String COMMON_ASYNC_EXECUTOR = "commonAsyncExecutor";

    /**
     * 创建并配置公共异步执行器Bean。
     * @param properties 公共异步线程池的配置属性
     * @return 配置好的线程池执行器
     */
    @Bean(name = COMMON_ASYNC_EXECUTOR)
    public Executor commonAsyncExecutor(CommonAsyncProperties properties) {
        // 创建线程池任务执行器
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数
        executor.setCorePoolSize(properties.getCorePoolSize());
        // 设置最大线程数
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        // 设置任务队列容量
        executor.setQueueCapacity(properties.getQueueCapacity());
        // 设置线程空闲存活时间
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        // 设置线程名前缀
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        // 设置任务装饰器，用于传递WebSocket用户上下文
        executor.setTaskDecorator(webSocketUserContextTaskDecorator());
        // 设置关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 设置等待任务完成的超时时间
        executor.setAwaitTerminationSeconds(30);
        // 初始化执行器
        executor.initialize();
        return executor;
    }

    /**
     * 透传 WebSocket 消息线程中的登录用户上下文，避免异步任务线程丢失当前用户。
     */
    private TaskDecorator webSocketUserContextTaskDecorator() {
        return runnable -> {
            String userId = WebSocketUserContextHolder.getUserId();
            String userName = WebSocketUserContextHolder.getUserName();
            return () -> {
                try {
                    WebSocketUserContextHolder.set(userId, userName);
                    runnable.run();
                } finally {
                    WebSocketUserContextHolder.clear();
                }
            };
        };
    }
}
