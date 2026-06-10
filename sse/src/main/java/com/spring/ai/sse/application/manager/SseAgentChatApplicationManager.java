package com.spring.ai.sse.application.manager;

import com.spring.ai.agent.application.manager.SimpleAgentChatApplicationManager;
import com.spring.ai.agent.application.manager.SimpleAgentSupportManager;
import com.spring.ai.agent.domain.request.SimpleAgentChatRequest;
import com.spring.ai.agent.domain.response.SimpleAgentWsEvent;
import com.spring.ai.common.config.async.CommonAsyncConfig;
import com.spring.ai.common.repository.enitiy.AgentSession;
import com.spring.ai.common.repository.enitiy.AgentSessionEvent;
import com.spring.ai.common.repository.service.AgentSessionEventService;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE Agent 对话应用管理器。
 *
 * <p>复用 simple-agent 已有任务编排与事件落库链路，通过轮询会话事件表向前端输出
 * text/event-stream，避免与 websocket 模块产生反向依赖。</p>
 */
@Component
public class SseAgentChatApplicationManager {

    private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;
    private static final long POLL_INTERVAL_MS = 260L;
    private static final int HEARTBEAT_INTERVAL_TICKS = 40;

    @Resource
    private SimpleAgentChatApplicationManager simpleAgentChatApplicationManager;

    @Resource
    private SimpleAgentSupportManager simpleAgentSupportManager;

    @Resource
    private AgentSessionEventService agentSessionEventService;

    @Resource
    @Qualifier(CommonAsyncConfig.COMMON_ASYNC_EXECUTOR)
    private Executor executor;

    public SseEmitter chat(SimpleAgentChatRequest request) {
        AgentSession session = simpleAgentSupportManager.requireSession(request.getSessionId());
        AtomicBoolean active = new AtomicBoolean(true);
        AtomicLong cursor = new AtomicLong(normalizeLastSequence(request.getLastReceivedEventSequence()));
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        bindLifecycle(emitter, active);
        simpleAgentChatApplicationManager.chat(request);
        executor.execute(() -> pollSessionEvents(session, cursor, active, emitter));
        return emitter;
    }

    private void pollSessionEvents(AgentSession session, AtomicLong cursor, AtomicBoolean active, SseEmitter emitter) {
        int heartbeatTicks = 0;
        boolean terminalSent = false;
        while (active.get()) {
            try {
                List<AgentSessionEvent> events = agentSessionEventService.listReplayEvents(
                        session.getId(),
                        session.getTenantId(),
                        cursor.get()
                );
                for (AgentSessionEvent event : events) {
                    sendAgentEvent(emitter, session, event);
                    cursor.set(Math.max(cursor.get(), normalizeLastSequence(event.getEventSequence())));
                    terminalSent = terminalSent || isTerminalEvent(event.getEventType());
                }
                if (terminalSent) {
                    completeEmitter(emitter, active);
                    return;
                }
                heartbeatTicks = heartbeatTicks + 1;
                if (heartbeatTicks >= HEARTBEAT_INTERVAL_TICKS) {
                    heartbeatTicks = 0;
                    sendHeartbeat(emitter);
                }
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                completeEmitter(emitter, active);
                return;
            } catch (Exception exception) {
                completeEmitterWithError(emitter, active, exception);
                return;
            }
        }
    }

    private void bindLifecycle(SseEmitter emitter, AtomicBoolean active) {
        emitter.onCompletion(() -> active.set(false));
        emitter.onTimeout(() -> completeEmitter(emitter, active));
        emitter.onError(error -> active.set(false));
    }

    private void sendAgentEvent(SseEmitter emitter, AgentSession session, AgentSessionEvent event) throws IOException {
        SimpleAgentWsEvent payload = simpleAgentSupportManager.buildReplayEvent(session, event);
        emitter.send(SseEmitter.event()
                .id(String.valueOf(event.getEventSequence()))
                .reconnectTime(3000L)
                .data(payload));
    }

    private void sendHeartbeat(SseEmitter emitter) throws IOException {
        emitter.send(SseEmitter.event().comment("agent-chat-keepalive"));
    }

    private void completeEmitter(SseEmitter emitter, AtomicBoolean active) {
        if (active.compareAndSet(true, false)) {
            emitter.complete();
        }
    }

    private void completeEmitterWithError(SseEmitter emitter, AtomicBoolean active, Throwable error) {
        if (active.compareAndSet(true, false)) {
            emitter.completeWithError(error);
        }
    }

    private boolean isTerminalEvent(String eventType) {
        return "AGENT_FINISH".equals(eventType)
                || "CHAT_ERROR".equals(eventType)
                || "METHOD_ERROR".equals(eventType);
    }

    private long normalizeLastSequence(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    private long normalizeLastSequence(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}

