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
 */
@Configuration
@EnableConfigurationProperties(CommonAsyncProperties.class)
public class CommonAsyncConfig {

    public static final String COMMON_ASYNC_EXECUTOR = "commonAsyncExecutor";

    @Bean(name = COMMON_ASYNC_EXECUTOR)
    public Executor commonAsyncExecutor(CommonAsyncProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setTaskDecorator(webSocketUserContextTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
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
