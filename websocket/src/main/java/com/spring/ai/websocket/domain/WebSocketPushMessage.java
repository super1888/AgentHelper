package com.spring.ai.websocket.domain;

import lombok.Builder;
import lombok.Value;

/**
 * WebSocket推送消息实体类
 * 使用@Value和@Builder注解，实现不可变对象和构建器模式
 */
@Value
@Builder
public class WebSocketPushMessage {

    // 事件类型，用于标识消息的具体业务类型
    String event;

    // 会话ID，用于标识特定的WebSocket连接
    String sessionId;

    // 目标地址，用于标识消息的接收者或路由
    String destination;

    // 消息数据，可以是任意类型的业务数据
    Object data;

    // 时间戳，用于记录消息发送的时间
    Long timestamp;
}
