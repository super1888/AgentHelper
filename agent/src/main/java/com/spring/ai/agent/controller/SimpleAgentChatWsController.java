package com.spring.ai.agent.controller;

import com.spring.ai.agent.domain.request.SimpleAgentChatRequest;
import com.spring.ai.agent.service.SimpleAgentChatService;
import jakarta.annotation.Resource;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 消息入口。
 * 前端将用户问题发送到 /app/agent/chat 后，由这里转交给 Agent 服务处理。
 */
@Controller
public class SimpleAgentChatWsController {

    @Resource
    private SimpleAgentChatService simpleAgentChatService;

    @MessageMapping("/agent/chat")
    public void chat(@Payload SimpleAgentChatRequest request) {
        simpleAgentChatService.chat(request);
    }
}
