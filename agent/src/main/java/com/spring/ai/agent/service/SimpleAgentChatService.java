package com.spring.ai.agent.service;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.spring.ai.agent.domain.request.SimpleAgentChatRequest;
import com.spring.ai.agent.domain.response.SimpleAgentWsEvent;
import com.spring.ai.agent.store.SimpleAgentRegistry;
import com.spring.ai.agent.store.SimpleAgentRegistry.StoredSimpleAgent;
import com.spring.ai.common.config.async.CommonAsyncConfig;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.websocket.annotation.WebSocketPush;
import com.spring.ai.websocket.service.WebSocketPushService;
import jakarta.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * 简单 Agent 会话服务。
 * 收到用户消息后，会先推送用户消息事件，再调用 Agent 流式回复并持续推送到前端。
 */
@Service
public class SimpleAgentChatService {

    @Resource
    private SimpleAgentRegistry simpleAgentRegistry;

    @Resource
    private WebSocketPushService webSocketPushService;

    @Resource
    @Qualifier(CommonAsyncConfig.COMMON_ASYNC_EXECUTOR)
    private Executor executor;

    @WebSocketPush(sessionId = "#p0.sessionId", sendResult = false, startEvent = "CHAT_START")
    public void chat(SimpleAgentChatRequest request) {
        validateChatRequest(request);
        StoredSimpleAgent storedSimpleAgent = simpleAgentRegistry.get(request.getAgentId());

        webSocketPushService.sendToSession(request.getSessionId(), "USER_MESSAGE",
                buildEvent(request.getAgentId(), request.getSessionId(), "USER_MESSAGE", request.getMessage()));

        CompletableFuture.runAsync(() -> {
            try {
                streamReply(storedSimpleAgent.getReactAgent(), request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    private void streamReply(ReactAgent reactAgent, SimpleAgentChatRequest request) throws Exception{
        StringBuilder fullContent = new StringBuilder();
        Flux<NodeOutput> stream = reactAgent.stream(request.getMessage().trim());
        stream.doOnNext(output -> handleStreamingOutput(output, request, fullContent))
                .doOnError(error -> webSocketPushService.sendToSession(request.getSessionId(), "CHAT_ERROR",
                        buildEvent(request.getAgentId(), request.getSessionId(), "CHAT_ERROR", error.getMessage())))
                .doOnComplete(() -> webSocketPushService.sendToSession(request.getSessionId(), "AGENT_FINISH",
                        buildEvent(request.getAgentId(), request.getSessionId(), "AGENT_FINISH", fullContent.toString())))
                .blockLast();
    }

    private void handleStreamingOutput(NodeOutput output, SimpleAgentChatRequest request, StringBuilder fullContent) {
        if (!(output instanceof StreamingOutput streamingOutput)) {
            return;
        }
        OutputType outputType = streamingOutput.getOutputType();
        Message message = streamingOutput.message();

        if (outputType == OutputType.AGENT_MODEL_STREAMING && message instanceof AssistantMessage assistantMessage) {
            String reasoning = stringify(assistantMessage.getMetadata().get("reasoningContent"));
            if (StringUtils.hasText(reasoning)) {
                webSocketPushService.sendToSession(request.getSessionId(), "AGENT_REASONING",
                        buildEvent(request.getAgentId(), request.getSessionId(), "AGENT_REASONING", reasoning));
            }

            String text = assistantMessage.getText();
            if (StringUtils.hasText(text)) {
                fullContent.append(text);
                webSocketPushService.sendToSession(request.getSessionId(), "AGENT_TOKEN",
                        buildEvent(request.getAgentId(), request.getSessionId(), "AGENT_TOKEN", text));
            }
            return;
        }

        if (outputType == OutputType.AGENT_TOOL_FINISHED && message instanceof ToolResponseMessage toolResponseMessage) {
            toolResponseMessage.getResponses().forEach(response ->
                    webSocketPushService.sendToSession(request.getSessionId(), "AGENT_TOOL",
                            buildEvent(request.getAgentId(), request.getSessionId(), "AGENT_TOOL",
                                    response.name() + ": " + stringify(response.responseData()))));
        }
    }

    private void validateChatRequest(SimpleAgentChatRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "chat request must not be null");
        }
        if (!StringUtils.hasText(request.getAgentId())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "agentId must not be blank");
        }
        if (!StringUtils.hasText(request.getSessionId())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "sessionId must not be blank");
        }
        if (!StringUtils.hasText(request.getMessage())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "message must not be blank");
        }
    }

    private SimpleAgentWsEvent buildEvent(String agentId, String sessionId, String event, Object data) {
        return SimpleAgentWsEvent.builder()
                .agentId(agentId)
                .sessionId(sessionId)
                .event(event)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
