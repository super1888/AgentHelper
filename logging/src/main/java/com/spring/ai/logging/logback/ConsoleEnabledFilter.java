package com.spring.ai.logging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 控制台日志开关过滤器。
 */
public class ConsoleEnabledFilter extends Filter<ILoggingEvent> {

    private boolean enabled = true;

    /**
     * 由 Logback XML 注入开关值。
     */
    public void setEnabled(String enabled) {
        this.enabled = Boolean.parseBoolean(enabled);
    }

    /**
     * 兼容 XML 中使用 consoleEnabled 作为属性名。
     */
    public void setConsoleEnabled(String enabled) {
        setEnabled(enabled);
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        return enabled ? FilterReply.NEUTRAL : FilterReply.DENY;
    }
}
