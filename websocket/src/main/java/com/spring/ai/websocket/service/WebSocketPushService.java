package com.spring.ai.websocket.service;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.websocket.config.WebSocketPushProperties;
import com.spring.ai.websocket.context.WebSocketPushContext;
import com.spring.ai.websocket.domain.WebSocketPushMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WebSocketPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketPushProperties properties;

    public WebSocketPushService(SimpMessagingTemplate messagingTemplate, WebSocketPushProperties properties) {
        this.messagingTemplate = messagingTemplate;
        this.properties = properties;
    }

    public void send(String destination, String event, Object data) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(destination)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "实时推送地址不能为空");
        }
        // 复用切面绑定的 sessionId，保证这里的手动推送与方法生命周期事件属于同一个会话。
        doSend(destination, event, data, WebSocketPushContext.getSessionId());
    }

    public void sendToSession(String sessionId, String event, Object data) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "会话编号不能为空");
        }
        doSend(buildSessionDestination(sessionId), event, data, sessionId);
    }

    public String buildSessionDestination(String sessionId) {
        return properties.getSessionDestinationPrefix() + "/" + sessionId;
    }

    private void doSend(String destination, String event, Object data, String sessionId) {
        messagingTemplate.convertAndSend(destination, WebSocketPushMessage.builder()
                .event(event)
                .sessionId(sessionId)
                .destination(destination)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build());
    }
}
