package com.spring.ai.sse.controller;

import com.spring.ai.agent.domain.request.SimpleAgentChatRequest;
import com.spring.ai.sse.application.manager.SseAgentChatApplicationManager;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent SSE 对话输出入口。
 *
 * <p>quickStart 会统一追加 /agentHelper 前缀，前端实际访问
 * /agentHelper/sse/agent/chat。</p>
 */
@RestController
@RequestMapping("/sse/agent")
public class SseAgentChatController {

    @Resource
    private SseAgentChatApplicationManager sseAgentChatApplicationManager;

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @RequestParam(required = false) String agentId,
            @RequestParam String sessionId,
            @RequestParam String message,
            @RequestParam(required = false) Long lastReceivedEventSequence
    ) {
        SimpleAgentChatRequest request = new SimpleAgentChatRequest();
        request.setAgentId(agentId);
        request.setSessionId(sessionId);
        request.setMessage(message);
        request.setLastReceivedEventSequence(lastReceivedEventSequence);
        return sseAgentChatApplicationManager.chat(request);
    }
}
