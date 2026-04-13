package com.spring.ai.websocket.context;

public final class WebSocketPushContext {

    /**
     * 在带有 @WebSocketPush 的方法执行期间，保存当前会话的 sessionId。
     * 这样业务代码在方法内部发送增量消息时，不需要层层透传目标地址或会话编号。
     */
    private static final ThreadLocal<String> SESSION_HOLDER = new ThreadLocal<>();

    private WebSocketPushContext() {
    }

    public static void bindSessionId(String sessionId) {
        SESSION_HOLDER.set(sessionId);
    }

    public static String getSessionId() {
        return SESSION_HOLDER.get();
    }

    public static void clear() {
        SESSION_HOLDER.remove();
    }
}
