package com.spring.ai.common.config.snowflakeid;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 雪花算法配置。
 * 后续如果部署到多节点环境，可以通过配置区分 workerId 和 datacenterId。
 */
@ConfigurationProperties(prefix = "app.snowflake")
public class SnowflakeIdProperties {

    /**
     * 工作节点编号，取值范围 0-31。
     */
    private long workerId = 1L;

    /**
     * 数据中心编号，取值范围 0-31。
     */
    private long datacenterId = 1L;

    public long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(long workerId) {
        this.workerId = workerId;
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public void setDatacenterId(long datacenterId) {
        this.datacenterId = datacenterId;
    }
}
