package com.spring.ai.user.application.provider;

import cn.dev33.satoken.exception.SaTokenContextException;
import cn.dev33.satoken.stp.StpUtil;
import com.spring.ai.common.constants.UserAuthConstants;
import com.spring.ai.common.providerInterface.UserProvider;
import com.spring.ai.common.web.WebSocketUserContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 基于 Sa-Token 的当前登录用户提供器。
 */
@Component
public class SaTokenUserProvider implements UserProvider {

    @Override
    public Long getCurrentUserId() {
        Long webSocketUserId = getWebSocketUserId();
        if (webSocketUserId != null) {
            return webSocketUserId;
        }
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (SaTokenContextException ignored) {
            // WebSocket 消息线程没有 Servlet 上下文时，回退到会话属性或默认值。
        }
        return 1L;
    }

    @Override
    public String getCurrentUserName() {
        String webSocketUserName = getWebSocketUserName();
        if (StringUtils.hasText(webSocketUserName)) {
            return webSocketUserName;
        }
        try {
            if (StpUtil.isLogin()) {
                return (String) StpUtil.getSession().get(UserAuthConstants.LOGIN_NAME);
            }
        } catch (SaTokenContextException ignored) {
            // WebSocket 消息线程没有 Servlet 上下文时，回退到会话属性或默认值。
        }
        return "admin";
    }

    /**
     * 优先从 WebSocket 会话属性中获取登录用户 ID。
     */
    private Long getWebSocketUserId() {
        String userId = WebSocketUserContextHolder.getUserId();
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        try {
            return Long.valueOf(userId.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 优先从 WebSocket 会话属性中获取登录用户名。
     */
    private String getWebSocketUserName() {
        return WebSocketUserContextHolder.getUserName();
    }
}
