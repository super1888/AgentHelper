package com.spring.ai.logging.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 企业级日志配置属性。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.logging")
public class AgentHelperLoggingProperties {

    /**
     * 日志根目录。
     */
    private String path = "./logs";

    /**
     * 业务应用名称。
     */
    private String appName = "agent-helper";

    /**
     * 是否开启控制台输出。
     */
    private boolean consoleEnabled = true;

    /**
     * 文件滚动策略。
     */
    private final Rolling rolling = new Rolling();

    /**
     * traceId 相关配置。
     */
    private final Trace trace = new Trace();

    /**
     * HTTP 访问日志配置。
     */
    private final Access access = new Access();

    /**
     * SQL 日志配置。
     */
    private final Sql sql = new Sql();

    @Getter
    @Setter
    public static class Rolling {

        /**
         * 单个日志文件最大大小。
         */
        private String maxFileSize = "100MB";

        /**
         * 日志保留天数。
         */
        private int maxHistory = 30;

        /**
         * 日志文件总大小上限。
         */
        private String totalSizeCap = "3GB";

        /**
         * 启动时是否清理历史文件。
         */
        private boolean cleanHistoryOnStart = false;
    }

    @Getter
    @Setter
    public static class Trace {

        /**
         * 上游链路标识请求头。
         */
        private String headerName = "X-Trace-Id";

        /**
         * 是否在响应头回写 traceId。
         */
        private boolean responseHeaderEnabled = true;
    }

    @Getter
    @Setter
    public static class Access {

        /**
         * 是否开启 HTTP 访问日志。
         */
        private boolean enabled = true;

        /**
         * 是否记录请求参数。
         */
        private boolean logRequestParameters = true;

        /**
         * 是否记录请求头。
         */
        private boolean logRequestHeaders = false;

        /**
         * 单项日志最大长度。
         */
        private int maxBodyLength = 1000;

        /**
         * 访问日志默认排除路径前缀。
         */
        private List<String> excludePathPrefixes = List.of("/favicon.ico", "/error", "/actuator");
    }

    @Getter
    @Setter
    public static class Sql {

        /**
         * 是否开启 SQL 日志。
         */
        private boolean enabled = false;

        /**
         * 慢 SQL 阈值，单位毫秒。
         */
        private long slowThresholdMs = 1000L;

        /**
         * 是否记录参数替换后的 SQL。
         */
        private boolean logParameters = true;

        /**
         * SQL 文本最大长度。
         */
        private int maxSqlLength = 4000;
    }
}
