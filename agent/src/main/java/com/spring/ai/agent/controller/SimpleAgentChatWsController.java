package com.spring.ai.agent.controller;

import com.spring.ai.agent.application.manager.SimpleAgentChatApplicationManager;
import com.spring.ai.agent.domain.request.SimpleAgentChatRequest;
import jakarta.annotation.Resource;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 聊天消息入口。
 *
 * <p>前端发送到 `/app/agent/chat` 的消息会先进入这里，
 * 再交给会话编排管理器处理。</p>
 */
@Controller
public class SimpleAgentChatWsController {

    @Resource
    private SimpleAgentChatApplicationManager simpleAgentChatApplicationManager;

    /**
     * 处理 WebSocket 聊天消息。
     *
     * @param request 聊天请求
     */
    @MessageMapping("/agent/chat")
    public void chat(@Payload SimpleAgentChatRequest request) {
        simpleAgentChatApplicationManager.chat(request);
    }
}
