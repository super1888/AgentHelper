package com.spring.ai.common.config.async;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 公共异步线程池配置。
 * 该类用于配置和管理应用程序中的公共异步线程池参数。
 * 通过@ConfigurationProperties注解，可以将配置文件中以"app.common.async"为前缀的属性绑定到该类的字段上。
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "app.common.async")
public class CommonAsyncProperties {

    /**
     * 核心线程池大小。
     * 表示线程池中保持的核心线程数量，默认值为（默认：CPU核心数）。
     * 即使线程池空闲，这些线程也不会被销毁。
     */
    private int corePoolSize = Runtime.getRuntime().availableProcessors();
    /**
     * 最大线程池大小。
     * 表示线程池中允许的最大线程数量，默认值为（默认：CPU核心数 * 2）。
     * 当任务队列满了之后，线程池会创建新线程来处理任务，直到达到最大线程数。
     */
    private int maxPoolSize = Runtime.getRuntime().availableProcessors() * 2;
    /**
     * 任务队列容量。
     * 表示线程池中任务队列的最大容量，默认值为200。
     * 当所有核心线程都在忙碌时，新任务会被放入队列中，直到队列满为止。
     */
    private int queueCapacity = 200;
    /**
     * 线程存活时间。
     * 表示线程池中非核心线程的空闲存活时间，单位为秒，默认值为60。
     * 超过这个时间后，非核心线程会被销毁。
     */
    private int keepAliveSeconds = 60;
    /**
     * 线程名称前缀。
     * 表示线程池中线程名称的前缀，默认值为"common-async-"。
     * 这个前缀加上线程编号可以形成唯一的线程名称，方便调试和监控。
     */
    private String threadNamePrefix = "common-async-";

}
