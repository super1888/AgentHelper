package com.spring.ai.agent.application.manager;

import com.spring.ai.agent.application.assmbler.SimpleAgentAssembler;
import com.spring.ai.agent.domain.response.SimpleAgentWsEvent;
import com.spring.ai.common.constants.SimpleAgentConstants;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.SyAgentSession;
import com.spring.ai.common.repository.enitiy.SyAgentTask;
import com.spring.ai.common.repository.service.SyAgentSessionEventService;
import com.spring.ai.common.repository.service.SyAgentSessionService;
import com.spring.ai.common.repository.service.SyAgentTaskService;
import com.spring.ai.websocket.service.WebSocketPushService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 会话写入管理器。
 *
 * <p>集中处理任务创建、事件落库、任务完成/失败状态更新，
 * 保证异步执行链路中的事务边界清晰且可控。</p>
 */
@Component
public class SimpleAgentChatPersistenceManager {

    private final Map<Long, Object> sessionLocks = new ConcurrentHashMap<>();

    @Resource
    private SyAgentSessionService syAgentSessionService;

    @Resource
    private SyAgentSessionEventService syAgentSessionEventService;

    @Resource
    private SyAgentTaskService syAgentTaskService;

    @Resource
    private SimpleAgentSupportManager simpleAgentSupportManager;

    @Resource
    private WebSocketPushService webSocketPushService;

    @Transactional(rollbackFor = Exception.class)
    public SyAgentTask createTask(SyAgentSession session, String requestMessage, Long sourceTaskId, Integer retryCount) {
        SyAgentTask runningTask = syAgentTaskService.getRunningTask(session.getId(), session.getTenantId());
        if (runningTask != null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.CONFLICT,
                    "there is already a running task in this session");
        }

        SyAgentTask task = SimpleAgentAssembler.toCreateTask(session, requestMessage, sourceTaskId, retryCount);
        syAgentTaskService.save(task);

        session.setSessionStatus(SimpleAgentConstants.SESSION_STATUS_ACTIVE);
        session.setConnectionStatus(SimpleAgentConstants.CONNECTION_STATUS_CONNECTED);
        session.setLastUserMessage(task.getRequestMessage());
        session.setLastConnectedTime(LocalDateTime.now());
        syAgentSessionService.updateById(session);
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishEvent(SyAgentSession session, SyAgentTask task, String eventType, Object data, Integer replayable) {
        Object lock = sessionLocks.computeIfAbsent(session.getId(), key -> new Object());
        SimpleAgentWsEvent wsEvent;
        synchronized (lock) {
            SyAgentSession freshSession = syAgentSessionService.getById(session.getId());
            long nextSequence = freshSession.getLastEventSequence() == null ? 1L : freshSession.getLastEventSequence() + 1;
            freshSession.setLastEventSequence(nextSequence);
            syAgentSessionService.updateById(freshSession);

            syAgentSessionEventService.save(SimpleAgentAssembler.toCreateSessionEvent(
                    freshSession,
                    task == null ? null : task.getId(),
                    eventType,
                    serializeEventData(data),
                    replayable,
                    nextSequence
            ));

            wsEvent = SimpleAgentAssembler.toWsEvent(
                    freshSession,
                    task == null ? null : task.getTaskCode(),
                    freshSession.getAgentVersionId(),
                    freshSession.getAgentVersionNo(),
                    eventType,
                    data,
                    nextSequence,
                    System.currentTimeMillis()
            );
        }
        webSocketPushService.sendToSession(session.getSessionCode(), eventType, wsEvent);
    }

    @Transactional(rollbackFor = Exception.class)
    public void handleTaskSuccess(SyAgentSession session, SyAgentTask task, String fullResponse) {
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
    public void handleTaskError(SyAgentSession session, SyAgentTask task, String errorMessage) {
        SyAgentTask freshTask = syAgentTaskService.getById(task.getId());
        SyAgentSession freshSession = syAgentSessionService.getById(session.getId());
        freshTask.setTaskStatus(SimpleAgentConstants.TASK_STATUS_FAILED);
        freshTask.setErrorMessage(errorMessage);
        syAgentTaskService.updateById(freshTask);

        freshSession.setSessionStatus(SimpleAgentConstants.SESSION_STATUS_FAILED);
        syAgentSessionService.updateById(freshSession);

        publishEvent(freshSession, freshTask, "CHAT_ERROR", errorMessage, 1);
    }

    public String serializeEventData(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof String text) {
            return text;
        }
        return simpleAgentSupportManager.toJson(data);
    }
}
