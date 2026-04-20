package com.spring.ai.websocket.config;

import com.spring.ai.user.config.WebSocketAuthChannelInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(WebSocketPushProperties.class)
public class WebSocketModuleConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketPushProperties properties;
    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

    public WebSocketModuleConfig(
            WebSocketPushProperties properties,
            WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor
    ) {
        this.properties = properties;
        this.webSocketAuthChannelInterceptor = webSocketAuthChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 当前先使用内存 Broker，便于快速接入。
        // 如果后续需要多实例部署，可以替换成外部消息 Broker，而业务调用方无需改动。
        registry.enableSimpleBroker(properties.getBrokerDestinationPrefix());
        registry.setApplicationDestinationPrefixes(properties.getAppDestinationPrefix());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 同时注册原生 WebSocket 端点和 SockJS 端点：
        // 1. 原生 WebSocket 便于 Apifox、Postman 这类调试工具直接连接
        // 2. SockJS 便于浏览器前端在兼容模式下接入
        registry.addEndpoint(properties.getEndpoint())
                .setAllowedOriginPatterns(properties.getAllowedOriginPatterns());

        registry.addEndpoint(properties.getEndpoint())
                .setAllowedOriginPatterns(properties.getAllowedOriginPatterns())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthChannelInterceptor);
    }
}
