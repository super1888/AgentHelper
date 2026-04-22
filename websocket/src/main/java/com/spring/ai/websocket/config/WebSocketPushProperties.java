package com.spring.ai.websocket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.websocket")
public class WebSocketPushProperties {

    private boolean enabled = true;

    private String endpoint = "/ws";

    private String brokerDestinationPrefix = "/topic";

    private String appDestinationPrefix = "/app";

    private String sessionDestinationPrefix = "/topic/session";

    private String allowedOriginPatterns = "*";

}
