package com.spring.ai.common.web;

/**
 * 文件用途：定义 WebSocket 会话中的登录用户上下文字段。
 *
 * <p>核心功能：统一维护 STOMP 会话属性中的用户标识与用户名键名，
 * 便于 WebSocket 消息处理阶段复用登录态。</p>
 */
public final class WebSocketUserContext {

    /**
     * WebSocket 会话中的登录用户 ID 属性键。
     */
    public static final String LOGIN_USER_ID_KEY = "wsLoginUserId";

    /**
     * WebSocket 会话中的登录用户名属性键。
     */
    public static final String LOGIN_USER_NAME_KEY = "wsLoginUserName";

    private WebSocketUserContext() {
    }
}
