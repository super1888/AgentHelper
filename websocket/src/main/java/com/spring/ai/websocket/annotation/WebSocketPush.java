package com.spring.ai.websocket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebSocketPush {

    /**
     * 自定义推送目标地址。
     * 如果不填写，则默认推送到 /topic/session/{sessionId}。
     */
    String destination() default "";

    /**
     * 用于从方法参数中解析 sessionId 的 SpEL 表达式。
     */
    String sessionId() default "";

    /**
     * 是否在方法执行前推送开始事件。
     */
    boolean sendStart() default true;

    /**
     * 是否在方法成功结束后推送结果事件。
     */
    boolean sendResult() default true;

    /**
     * 是否在方法抛出异常时推送异常事件。
     */
    boolean sendError() default true;

    String startEvent() default "METHOD_START";

    String resultEvent() default "METHOD_RESULT";

    String errorEvent() default "METHOD_ERROR";
}
