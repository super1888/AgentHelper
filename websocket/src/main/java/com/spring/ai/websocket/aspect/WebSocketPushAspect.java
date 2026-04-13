package com.spring.ai.websocket.aspect;

import com.spring.ai.websocket.annotation.WebSocketPush;
import com.spring.ai.websocket.config.WebSocketPushProperties;
import com.spring.ai.websocket.context.WebSocketPushContext;
import com.spring.ai.websocket.service.WebSocketPushService;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Aspect
@Component
public class WebSocketPushAspect {

    private final WebSocketPushService webSocketPushService;
    private final WebSocketPushProperties properties;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public WebSocketPushAspect(WebSocketPushService webSocketPushService, WebSocketPushProperties properties) {
        this.webSocketPushService = webSocketPushService;
        this.properties = properties;
    }

    @Around("@annotation(webSocketPush)")
    public Object around(ProceedingJoinPoint joinPoint, WebSocketPush webSocketPush) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        // 在目标方法执行前先解析 sessionId 和目标地址，
        // 这样切面自动推送的生命周期事件与业务代码中的手动推送会落到同一个会话通道。
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MethodBasedEvaluationContext context =
                new MethodBasedEvaluationContext(joinPoint.getTarget(), method, joinPoint.getArgs(), parameterNameDiscoverer);

        String sessionId = resolveExpression(webSocketPush.sessionId(), context);
        String destination = resolveDestination(webSocketPush, context, sessionId);
        if (StringUtils.hasText(sessionId)) {
            WebSocketPushContext.bindSessionId(sessionId);
        }

        try {
            if (webSocketPush.sendStart() && StringUtils.hasText(destination)) {
                webSocketPushService.send(destination, webSocketPush.startEvent(), buildLifecyclePayload(method, "START", null));
            }

            Object result = joinPoint.proceed();

            if (webSocketPush.sendResult() && StringUtils.hasText(destination)) {
                webSocketPushService.send(destination, webSocketPush.resultEvent(), buildLifecyclePayload(method, "SUCCESS", result));
            }
            return result;
        } catch (Throwable throwable) {
            if (webSocketPush.sendError() && StringUtils.hasText(destination)) {
                webSocketPushService.send(destination, webSocketPush.errorEvent(),
                        buildLifecyclePayload(method, "ERROR", throwable.getMessage()));
            }
            throw throwable;
        } finally {
            // 清理线程上下文，避免当前请求的 sessionId 污染后续请求。
            WebSocketPushContext.clear();
        }
    }

    private String resolveDestination(WebSocketPush webSocketPush, MethodBasedEvaluationContext context, String sessionId) {
        String destination = resolveExpression(webSocketPush.destination(), context);
        if (StringUtils.hasText(destination)) {
            return destination;
        }
        if (StringUtils.hasText(sessionId)) {
            return properties.getSessionDestinationPrefix() + "/" + sessionId;
        }
        return null;
    }

    private String resolveExpression(String expression, MethodBasedEvaluationContext context) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        // 普通字符串按字面量处理，只有明显是 SpEL 的表达式才进入解析。
        if (!expression.contains("#") && !expression.contains("'")) {
            return expression;
        }
        Object value = parser.parseExpression(expression).getValue(context);
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> buildLifecyclePayload(Method method, String status, Object data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("method", method.getDeclaringClass().getSimpleName() + "." + method.getName());
        payload.put("status", status);
        payload.put("data", data);
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}
