package com.spring.ai.agent.controller;

import com.spring.ai.agent.domain.request.SimpleAgentChatRequest;
import com.spring.ai.agent.service.SimpleAgentChatService;
import jakarta.annotation.Resource;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 消息入口。
 *
 * <p>前端发送到 `/app/agent/chat` 的消息会先进入这里，
 * 再交给会话服务处理。</p>
 */
@Controller
public class SimpleAgentChatWsController {

    @Resource
    private SimpleAgentChatService simpleAgentChatService;

    /**
     * 处理 WebSocket 聊天消息。
     */
    @MessageMapping("/agent/chat")
    public void chat(@Payload SimpleAgentChatRequest request) {
        simpleAgentChatService.chat(request);
    }
}

