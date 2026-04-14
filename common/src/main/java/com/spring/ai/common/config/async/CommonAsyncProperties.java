package com.spring.ai.common.config.async;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 公共异步线程池配置。
 */
@ConfigurationProperties(prefix = "app.common.async")
public class CommonAsyncProperties {

    private int corePoolSize = 4;
    private int maxPoolSize = 8;
    private int queueCapacity = 200;
    private int keepAliveSeconds = 60;
    private String threadNamePrefix = "common-async-";

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }
}
