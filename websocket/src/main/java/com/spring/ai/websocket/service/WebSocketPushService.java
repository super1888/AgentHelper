package com.spring.ai.websocket.service;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.websocket.config.WebSocketPushProperties;
import com.spring.ai.websocket.context.WebSocketPushContext;
import com.spring.ai.websocket.domain.WebSocketPushMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * WebSocket推送服务类，用于处理WebSocket消息的推送功能
 */
@Service
public class WebSocketPushService {

    // 使用SimpMessagingTemplate进行消息推送
    private final SimpMessagingTemplate messagingTemplate;
    // WebSocket推送配置属性
    private final WebSocketPushProperties properties;

    /**
     * 构造函数，注入所需依赖
     *
     * @param messagingTemplate 消息模板，用于发送消息
     * @param properties        WebSocket推送配置属性
     */
    public WebSocketPushService(SimpMessagingTemplate messagingTemplate, WebSocketPushProperties properties) {
        this.messagingTemplate = messagingTemplate;
        this.properties = properties;
    }

    /**
     * 发送WebSocket消息到指定目的地
     *
     * @param destination 推送目的地
     * @param event       事件类型
     * @param data        要推送的数据
     */
    public void send(String destination, String event, Object data) {
        // 检查WebSocket推送功能是否启用
        if (!properties.isEnabled()) {
            return;
        }
        // 验证目的地不能为空
        if (!StringUtils.hasText(destination)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "实时推送地址不能为空");
        }
        // 复用切面绑定的 sessionId，保证这里的手动推送与方法生命周期事件属于同一个会话。
        doSend(destination, event, data, WebSocketPushContext.getSessionId());
    }

    /**
     * 向指定会话发送消息
     *
     * @param sessionId 会话编号，不能为空
     * @param event     事件类型
     * @param data      要发送的数据
     * @throws BusinessException 当会话编号为空时抛出异常
     */
    public void sendToSession(String sessionId, String event, Object data) {
        // 检查会话编号是否为空，如果为空则抛出业务异常
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "会话编号不能为空");
        }
        // 构建会话目标并发送消息
        doSend(buildSessionDestination(sessionId), event, data, sessionId);
    }

    /**
     * 构建会话目标地址的方法 通过会话ID和配置的前缀组合成完整的目标地址
     *
     * @param sessionId 会话的唯一标识符
     * @return 返回构建后的完整目标地址，格式为"前缀/会话ID"
     */
    public String buildSessionDestination(String sessionId) {
        // 获取配置的会话目标前缀，并与会话ID拼接
        return properties.getSessionDestinationPrefix() + "/" + sessionId;
    }


    /**
     * 发送WebSocket消息的方法
     *
     * @param destination 目标地址
     * @param event       事件类型
     * @param data        要发送的数据
     * @param sessionId   会话ID
     */
    private void doSend(String destination, String event, Object data, String sessionId) {

        // 使用messagingTemplate发送消息
        // 构建WebSocketPushMessage对象，包含事件类型、会话ID、目标地址、数据和发送时间戳
        messagingTemplate.convertAndSend(destination, WebSocketPushMessage.builder()
                .event(event)         // 设置事件类型
                .sessionId(sessionId)                  // 设置会话ID
                .destination(destination)  // 设置目标地址
                .data(data)                          // 设置要发送的数据
                .timestamp(System.currentTimeMillis())  // 设置当前时间戳作为发送时间
                .build());           // 构建消息对象并发送
    }
}
