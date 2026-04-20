package com.spring.ai.user.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.spring.ai.common.constants.UserAuthConstants;
import com.spring.ai.common.web.WebSocketUserContext;
import com.spring.ai.common.web.WebSocketUserContextHolder;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：为 WebSocket STOMP 入站消息补充登录用户上下文。
 *
 * <p>核心功能：在 CONNECT 阶段从前端 header 中读取 Sa-Token，
 * 解析登录用户并写入 WebSocket session attributes，
 * 供后续消息处理线程安全复用。</p>
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor, ExecutorChannelInterceptor {

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        bindUserContext(accessor);
        if (accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String tokenValue = resolveTokenValue(accessor);
        if (!StringUtils.hasText(tokenValue)) {
            return message;
        }

        Object loginId = StpUtil.getLoginIdByToken(tokenValue);
        if (loginId == null) {
            return message;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        sessionAttributes.put(WebSocketUserContext.LOGIN_USER_ID_KEY, String.valueOf(loginId));
        Object loginName = null;
        if (StpUtil.getSessionByLoginId(loginId) != null) {
            loginName = StpUtil.getSessionByLoginId(loginId).get(UserAuthConstants.LOGIN_NAME);
        }
        if (loginName != null) {
            sessionAttributes.put(WebSocketUserContext.LOGIN_USER_NAME_KEY, String.valueOf(loginName));
        }
        bindUserContext(accessor);
        return message;
    }

    @Override
    public void afterSendCompletion(
            @NonNull Message<?> message,
            @NonNull MessageChannel channel,
            boolean sent,
            Exception ex
    ) {
        WebSocketUserContextHolder.clear();
    }

    @Override
    public Message<?> beforeHandle(
            @NonNull Message<?> message,
            @NonNull MessageChannel channel,
            @NonNull MessageHandler handler
    ) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        bindUserContext(accessor);
        return message;
    }

    @Override
    public void afterMessageHandled(
            @NonNull Message<?> message,
            @NonNull MessageChannel channel,
            @NonNull MessageHandler handler,
            Exception ex
    ) {
        WebSocketUserContextHolder.clear();
    }

    /**
     * 从 STOMP 原生 header 中解析 Sa-Token 原始值。
     */
    private String resolveTokenValue(StompHeaderAccessor accessor) {
        String headerName = StpUtil.getTokenName();
        String rawValue = accessor.getFirstNativeHeader(headerName);
        if (!StringUtils.hasText(rawValue)) {
            rawValue = accessor.getFirstNativeHeader("Authorization");
        }
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }

        String normalizedValue = rawValue.trim();
        String tokenPrefix = SaManager.getConfig().getTokenPrefix();
        if (StringUtils.hasText(tokenPrefix)) {
            String fullPrefix = tokenPrefix + " ";
            if (normalizedValue.startsWith(fullPrefix)) {
                return normalizedValue.substring(fullPrefix.length()).trim();
            }
        }
        return normalizedValue;
    }

    /**
     * 将当前 STOMP 会话中的登录用户绑定到消息处理线程。
     */
    private void bindUserContext(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        Object userId = sessionAttributes.get(WebSocketUserContext.LOGIN_USER_ID_KEY);
        Object userName = sessionAttributes.get(WebSocketUserContext.LOGIN_USER_NAME_KEY);
        WebSocketUserContextHolder.set(
                userId == null ? null : String.valueOf(userId),
                userName == null ? null : String.valueOf(userName)
        );
    }
}
