package com.spring.ai.common.web;

/**
 * 文件用途：维护 WebSocket 消息处理线程中的当前用户上下文。
 *
 * <p>核心功能：在 STOMP 入站消息处理期间缓存当前登录用户，
 * 供非 Servlet 场景下的公共用户提供器读取。</p>
 */
public final class WebSocketUserContextHolder {

    private static final ThreadLocal<String> LOGIN_USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> LOGIN_USER_NAME_HOLDER = new ThreadLocal<>();

    private WebSocketUserContextHolder() {
    }

    /**
     * 设置当前消息线程的 WebSocket 用户上下文。
     */
    public static void set(String userId, String userName) {
        LOGIN_USER_ID_HOLDER.set(userId);
        LOGIN_USER_NAME_HOLDER.set(userName);
    }

    /**
     * 获取当前消息线程中的登录用户 ID。
     */
    public static String getUserId() {
        return LOGIN_USER_ID_HOLDER.get();
    }

    /**
     * 获取当前消息线程中的登录用户名。
     */
    public static String getUserName() {
        return LOGIN_USER_NAME_HOLDER.get();
    }

    /**
     * 清理当前消息线程中的 WebSocket 用户上下文。
     */
    public static void clear() {
        LOGIN_USER_ID_HOLDER.remove();
        LOGIN_USER_NAME_HOLDER.remove();
    }
}
