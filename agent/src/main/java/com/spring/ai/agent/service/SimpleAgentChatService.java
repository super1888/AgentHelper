package com.spring.ai.agent.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.spring.ai.common.constants.SimpleAgentConstants;
import com.spring.ai.agent.domain.request.SimpleAgentChatRequest;
import com.spring.ai.agent.domain.request.SimpleAgentRecoverRequest;
import com.spring.ai.agent.domain.response.SimpleAgentRecoverResponse;
import com.spring.ai.agent.domain.response.SimpleAgentWsEvent;
import com.spring.ai.common.config.async.CommonAsyncConfig;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.SyAgent;
import com.spring.ai.common.repository.enitiy.SyAgentSession;
import com.spring.ai.common.repository.enitiy.SyAgentSessionEvent;
import com.spring.ai.common.repository.enitiy.SyAgentTask;
import com.spring.ai.common.repository.enitiy.SyAgentVersion;
import com.spring.ai.common.repository.service.SyAgentService;
import com.spring.ai.common.repository.service.SyAgentSessionEventService;
import com.spring.ai.common.repository.service.SyAgentSessionService;
import com.spring.ai.common.repository.service.SyAgentTaskService;
import com.spring.ai.common.repository.service.SyAgentVersionService;
import com.spring.ai.websocket.annotation.WebSocketPush;
import com.spring.ai.websocket.service.WebSocketPushService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * Simple Agent 会话服务。
 */
/**
 * Simple Agent 会话服务。
 *
 * <p>负责消息投递、事件落库、任务执行、失败恢复和断线补发。</p>
 */
@Service
public class SimpleAgentChatService {

    private final Map<Long, Object> sessionLocks = new ConcurrentHashMap<>();

    @Resource
    private SyAgentService syAgentService;

    @Resource
    private SyAgentSessionService syAgentSessionService;

    @Resource
    private SyAgentSessionEventService syAgentSessionEventService;

    @Resource
    private SyAgentTaskService syAgentTaskService;

    @Resource
    private SyAgentVersionService syAgentVersionService;

    @Resource
    private SimpleAgentSupportService simpleAgentSupportService;

    @Resource
    private SimpleAgentRuntimeService simpleAgentRuntimeService;

    @Resource
    private WebSocketPushService webSocketPushService;

    @Resource
    @Qualifier(CommonAsyncConfig.COMMON_ASYNC_EXECUTOR)
    private Executor executor;

    @WebSocketPush(sessionId = "#p0.sessionId", sendResult = false, startEvent = "CHAT_START")
    public void chat(SimpleAgentChatRequest request) {
        validateChatRequest(request);
        SyAgentSession session = simpleAgentSupportService.requireSession(request.getSessionId());
        validateSessionForChat(session, request.getAgentId());
        if (request.getLastReceivedEventSequence() != null) {
            // 先补发缺失事件，再处理新消息，避免前端消息流断层。
            replayMissedEvents(session, request.getLastReceivedEventSequence());
        }

        SyAgentTask task = createTask(session, request.getMessage(), null);
        // 用户输入先写入事件流，再异步执行模型任务。
        publishEvent(session, task, "USER_MESSAGE", request.getMessage(), 1);

        CompletableFuture.runAsync(() -> executeTask(session.getSessionCode(), task.getTaskCode()), executor);
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentRecoverResponse recoverTask(String sessionCode, SimpleAgentRecoverRequest request) {
        SyAgentSession session = simpleAgentSupportService.requireSession(sessionCode);
        SyAgentTask failedTask = resolveRecoverTask(session, request);

        SyAgentTask recoverTask = createTask(session, failedTask.getRequestMessage(), failedTask.getId());
        publishEvent(session, recoverTask, "TASK_RECOVER", failedTask.getTaskCode(), 1);
        CompletableFuture.runAsync(() -> executeTask(session.getSessionCode(), recoverTask.getTaskCode()), executor);

        return SimpleAgentRecoverResponse.builder()
                .sessionId(session.getSessionCode())
                .taskId(recoverTask.getTaskCode())
                .taskStatus(recoverTask.getTaskStatus())
                .message("recover task accepted")
                .build();
    }

    private void executeTask(String sessionCode, String taskCode) {
        SyAgentSession session = syAgentSessionService.getOne(Wrappers.lambdaQuery(SyAgentSession.class)
                .eq(SyAgentSession::getSessionCode, sessionCode)
                .last("limit 1"));
        SyAgentTask task = syAgentTaskService.getOne(Wrappers.lambdaQuery(SyAgentTask.class)
                .eq(SyAgentTask::getTaskCode, taskCode)
                .last("limit 1"));
        if (session == null || task == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "session or task not found");
        }
        // 异步线程不依赖登录态上下文，直接按主数据查询运行。
        SyAgent agent = syAgentService.getById(session.getAgentId());
        SyAgentVersion version = syAgentVersionService.getById(session.getAgentVersionId());
        if (agent == null || version == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "agent or version not found");
        }
        ReactAgent reactAgent = simpleAgentRuntimeService.getOrCreate(agent, version);

        StringBuilder fullContent = new StringBuilder();
        try {
            Flux<NodeOutput> stream = reactAgent.stream(task.getRequestMessage().trim());
            stream.doOnNext(output -> handleStreamingOutput(output, session, task, fullContent))
                    .doOnComplete(() -> handleTaskSuccess(session, task, fullContent.toString()))
                    .blockLast();
        } catch (Exception e) {
            handleTaskError(session, task, e);
        }
    }

    private void handleStreamingOutput(NodeOutput output, SyAgentSession session, SyAgentTask task, StringBuilder fullContent) {
        if (!(output instanceof StreamingOutput streamingOutput)) {
            return;
        }
        OutputType outputType = streamingOutput.getOutputType();
        Message message = streamingOutput.message();

        if (outputType == OutputType.AGENT_MODEL_STREAMING && message instanceof AssistantMessage assistantMessage) {
            String reasoning = stringify(assistantMessage.getMetadata().get("reasoningContent"));
            if (StringUtils.hasText(reasoning)) {
                publishEvent(session, task, "AGENT_REASONING", reasoning, 1);
            }

            String text = assistantMessage.getText();
            if (StringUtils.hasText(text)) {
                fullContent.append(text);
                publishEvent(session, task, "AGENT_TOKEN", text, 1);
            }
            return;
        }

        if (outputType == OutputType.AGENT_TOOL_FINISHED && message instanceof ToolResponseMessage toolResponseMessage) {
            toolResponseMessage.getResponses().forEach(response ->
                    publishEvent(session, task, "AGENT_TOOL",
                            response.name() + ": " + stringify(response.responseData()), 1));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected void handleTaskSuccess(SyAgentSession session, SyAgentTask task, String fullResponse) {
        SyAgentTask freshTask = syAgentTaskService.getById(task.getId());
        SyAgentSession freshSession = syAgentSessionService.getById(session.getId());
        freshTask.setTaskStatus(SimpleAgentConstants.TASK_STATUS_SUCCESS);
        freshTask.setResponseMessage(fullResponse);
        freshTask.setErrorMessage(null);
        syAgentTaskService.updateById(freshTask);

        freshSession.setSessionStatus(SimpleAgentConstants.SESSION_STATUS_ACTIVE);
        freshSession.setConnectionStatus(SimpleAgentConstants.CONNECTION_STATUS_CONNECTED);
        freshSession.setLastAssistantMessage(fullResponse);
        syAgentSessionService.updateById(freshSession);
        publishEvent(freshSession, freshTask, "AGENT_FINISH", fullResponse, 1);
    }

    @Transactional(rollbackFor = Exception.class)
    protected void handleTaskError(SyAgentSession session, SyAgentTask task, Throwable throwable) {
        SyAgentTask freshTask = syAgentTaskService.getById(task.getId());
        SyAgentSession freshSession = syAgentSessionService.getById(session.getId());
        freshTask.setTaskStatus(SimpleAgentConstants.TASK_STATUS_FAILED);
        freshTask.setErrorMessage(throwable.getMessage());
        syAgentTaskService.updateById(freshTask);

        freshSession.setSessionStatus(SimpleAgentConstants.SESSION_STATUS_FAILED);
        syAgentSessionService.updateById(freshSession);
        publishEvent(freshSession, freshTask, "CHAT_ERROR", throwable.getMessage(), 1);
    }

    @Transactional(rollbackFor = Exception.class)
    protected SyAgentTask createTask(SyAgentSession session, String requestMessage, Long sourceTaskId) {
        SyAgentTask runningTask = syAgentTaskService.getRunningTask(session.getId(), session.getTenantId());
        if (runningTask != null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.CONFLICT,
                    "there is already a running task in this session");
        }

        // 同一会话同时只允许一个运行中任务，避免事件序号和输出流混乱。
        SyAgentTask task = new SyAgentTask();
        task.setTaskCode(UUID.randomUUID().toString());
        task.setSourceTaskId(sourceTaskId);
        task.setSessionId(session.getId());
        task.setSessionCode(session.getSessionCode());
        task.setAgentId(session.getAgentId());
        task.setAgentVersionId(session.getAgentVersionId());
        task.setTenantId(session.getTenantId());
        task.setOwnerUserId(session.getOwnerUserId());
        task.setTaskStatus(SimpleAgentConstants.TASK_STATUS_RUNNING);
        task.setRequestMessage(requestMessage.trim());
        task.setRetryCount(resolveRetryCount(sourceTaskId));
        syAgentTaskService.save(task);

        session.setSessionStatus(SimpleAgentConstants.SESSION_STATUS_ACTIVE);
        session.setConnectionStatus(SimpleAgentConstants.CONNECTION_STATUS_CONNECTED);
        session.setLastUserMessage(requestMessage.trim());
        session.setLastConnectedTime(LocalDateTime.now());
        syAgentSessionService.updateById(session);
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    protected void publishEvent(SyAgentSession session, SyAgentTask task, String eventType, Object data, Integer replayable) {
        Object lock = sessionLocks.computeIfAbsent(session.getId(), key -> new Object());
        SimpleAgentWsEvent wsEvent;
        synchronized (lock) {
            // 单会话内串行分配事件序号，才能可靠支持断线补发。
            SyAgentSession freshSession = syAgentSessionService.getById(session.getId());
            long nextSequence = freshSession.getLastEventSequence() == null ? 1L : freshSession.getLastEventSequence() + 1;
            freshSession.setLastEventSequence(nextSequence);
            syAgentSessionService.updateById(freshSession);

            SyAgentSessionEvent event = new SyAgentSessionEvent();
            event.setSessionId(freshSession.getId());
            event.setSessionCode(freshSession.getSessionCode());
            event.setAgentId(freshSession.getAgentId());
            event.setAgentVersionId(freshSession.getAgentVersionId());
            event.setTenantId(freshSession.getTenantId());
            event.setTaskId(task == null ? null : task.getId());
            event.setEventSequence(nextSequence);
            event.setEventType(eventType);
            event.setEventBody(serializeEventData(data));
            event.setReplayable(replayable);
            syAgentSessionEventService.save(event);

            wsEvent = buildEvent(freshSession, task, eventType, data, nextSequence);
        }
        webSocketPushService.sendToSession(session.getSessionCode(), eventType, wsEvent);
    }

    private void replayMissedEvents(SyAgentSession session, Long lastReceivedSequence) {
        syAgentSessionEventService.listReplayEvents(session.getId(), session.getTenantId(), lastReceivedSequence)
                .forEach(event -> webSocketPushService.sendToSession(session.getSessionCode(), event.getEventType(),
                        buildReplayEvent(session, event)));
    }

    private void validateChatRequest(SimpleAgentChatRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "chat request must not be null");
        }
        if (!StringUtils.hasText(request.getSessionId())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "sessionId must not be blank");
        }
        if (!StringUtils.hasText(request.getMessage())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "message must not be blank");
        }
    }

    private void validateSessionForChat(SyAgentSession session, String agentCode) {
        if (SimpleAgentConstants.SESSION_STATUS_CLOSED.equals(session.getSessionStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "session has been closed");
        }
        if (StringUtils.hasText(agentCode) && !agentCode.equals(session.getAgentCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "session does not match current agent");
        }
    }

    private SyAgentTask resolveRecoverTask(SyAgentSession session, SimpleAgentRecoverRequest request) {
        if (request != null && StringUtils.hasText(request.getTaskId())) {
            SyAgentTask task = simpleAgentSupportService.requireTask(request.getTaskId());
            if (!task.getSessionId().equals(session.getId())) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                        "task does not belong to current session");
            }
            if (!SimpleAgentConstants.TASK_STATUS_FAILED.equals(task.getTaskStatus())) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                        "only failed task can be recovered");
            }
            return task;
        }

        SyAgentTask failedTask = syAgentTaskService.getLatestFailedTask(session.getId(), session.getTenantId());
        if (failedTask == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "failed task not found");
        }
        return failedTask;
    }

    private SimpleAgentWsEvent buildEvent(
            SyAgentSession session,
            SyAgentTask task,
            String event,
            Object data,
            Long eventSequence
    ) {
        return SimpleAgentWsEvent.builder()
                .agentId(session.getAgentCode())
                .sessionId(session.getSessionCode())
                .taskId(task == null ? null : task.getTaskCode())
                .agentVersionId(session.getAgentVersionId())
                .agentVersionNo(session.getAgentVersionNo())
                .eventSequence(eventSequence)
                .event(event)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private SimpleAgentWsEvent buildReplayEvent(SyAgentSession session, SyAgentSessionEvent event) {
        return SimpleAgentWsEvent.builder()
                .agentId(session.getAgentCode())
                .sessionId(session.getSessionCode())
                .taskId(resolveTaskCode(event.getTaskId()))
                .agentVersionId(event.getAgentVersionId())
                .agentVersionNo(session.getAgentVersionNo())
                .eventSequence(event.getEventSequence())
                .event(event.getEventType())
                .data(event.getEventBody())
                .timestamp(event.getCreateTime() == null ? System.currentTimeMillis() : event.getCreateTime().atZone(
                        java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                .build();
    }

    private String serializeEventData(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof String text) {
            return text;
        }
        return simpleAgentSupportService.toJson(data);
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String resolveTaskCode(Long taskId) {
        if (taskId == null) {
            return null;
        }
        SyAgentTask task = syAgentTaskService.getById(taskId);
        return task == null ? String.valueOf(taskId) : task.getTaskCode();
    }

    private int resolveRetryCount(Long sourceTaskId) {
        if (sourceTaskId == null) {
            return 0;
        }
        SyAgentTask sourceTask = syAgentTaskService.getById(sourceTaskId);
        if (sourceTask == null || sourceTask.getRetryCount() == null) {
            return 1;
        }
        return sourceTask.getRetryCount() + 1;
    }
}
