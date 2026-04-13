package com.spring.ai.websocket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.websocket")
public class WebSocketPushProperties {

    private boolean enabled = true;

    private String endpoint = "/ws";

    private String brokerDestinationPrefix = "/topic";

    private String appDestinationPrefix = "/app";

    private String sessionDestinationPrefix = "/topic/session";

    private String allowedOriginPatterns = "*";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBrokerDestinationPrefix() {
        return brokerDestinationPrefix;
    }

    public void setBrokerDestinationPrefix(String brokerDestinationPrefix) {
        this.brokerDestinationPrefix = brokerDestinationPrefix;
    }

    public String getAppDestinationPrefix() {
        return appDestinationPrefix;
    }

    public void setAppDestinationPrefix(String appDestinationPrefix) {
        this.appDestinationPrefix = appDestinationPrefix;
    }

    public String getSessionDestinationPrefix() {
        return sessionDestinationPrefix;
    }

    public void setSessionDestinationPrefix(String sessionDestinationPrefix) {
        this.sessionDestinationPrefix = sessionDestinationPrefix;
    }

    public String getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(String allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }
}
