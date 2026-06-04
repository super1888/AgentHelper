package com.spring.ai.gateway.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 网关访问控制配置，统一维护免登录路径和鉴权开关。
 */
@Component
@ConfigurationProperties(prefix = "gateway.access")
public class GatewayAccessProperties {

    private boolean enabled = true;

    private List<String> publicPaths = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}