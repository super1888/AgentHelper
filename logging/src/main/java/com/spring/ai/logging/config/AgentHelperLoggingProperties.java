package com.spring.ai.logging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 企业级日志配置属性。
 */
@Getter
@ConfigurationProperties(prefix = "app.logging")
public class AgentHelperLoggingProperties {

    /**
     * 日志根目录。
     */
    @Setter
    private String path = "./logs";

    /**
     * 业务应用名称。
     */
    @Setter
    private String appName = "agent-helper";

    /**
     * 是否开启控制台日志。
     */
    @Setter
    private boolean consoleEnabled = true;

    /**
     * 滚动策略配置。
     */
    private final Rolling rolling = new Rolling();

    /**
     * 请求链路追踪配置。
     */
    private final Trace trace = new Trace();

    @Setter
    @Getter
    public static class Rolling {

        /**
         * 单文件最大体积。
         */
        private String maxFileSize = "100MB";

        /**
         * 保留天数。
         */
        private int maxHistory = 30;

        /**
         * 日志总容量上限。
         */
        private String totalSizeCap = "3GB";

        /**
         * 启动时是否清理历史文件。
         */
        private boolean cleanHistoryOnStart = false;

    }

    @Setter
    @Getter
    public static class Trace {

        /**
         * 传入链路标识请求头。
         */
        private String headerName = "X-Trace-Id";

        /**
         * 是否回写链路标识到响应头。
         */
        private boolean responseHeaderEnabled = true;

    }
}
