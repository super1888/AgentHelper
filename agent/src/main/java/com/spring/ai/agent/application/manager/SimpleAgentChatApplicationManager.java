package com.spring.ai.agent.application.manager;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring.ai.agent.application.assmbler.SimpleAgentAssembler;
import com.spring.ai.agent.domain.dto.SimpleAgentVersionConfigDTO;
import com.spring.ai.agent.domain.request.SimpleAgentChatRequest;
import com.spring.ai.agent.domain.request.SimpleAgentRecoverRequest;
import com.spring.ai.agent.domain.response.SimpleAgentRecoverResponse;
import com.spring.ai.agent.domain.response.SimpleAgentWsEvent;
import com.spring.ai.common.config.async.CommonAsyncConfig;
import com.spring.ai.common.constants.SimpleAgentConstants;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.Agent;
import com.spring.ai.common.repository.enitiy.AgentSession;
import com.spring.ai.common.repository.enitiy.AgentSessionEvent;
import com.spring.ai.common.repository.enitiy.AgentTask;
import com.spring.ai.common.repository.enitiy.AgentVersion;
import com.spring.ai.common.repository.service.AgentService;
import com.spring.ai.common.repository.service.AgentSessionEventService;
import com.spring.ai.common.repository.service.AgentSessionService;
import com.spring.ai.common.repository.service.AgentTaskService;
import com.spring.ai.common.repository.service.AgentVersionService;
import com.spring.ai.hooks.application.manager.HookRuntimeManager;
import com.spring.ai.hooks.domain.dto.HookRuntimeResultDTO;
import com.spring.ai.websocket.annotation.WebSocketPush;
import com.spring.ai.websocket.service.WebSocketPushService;
import jakarta.annotation.Resource;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * Simple Agent 会话编排管理器。
 *
 * <p>负责聊天消息接入、事件补发、失败恢复以及异步任务调度。</p>
 */
@Component
public class SimpleAgentChatApplicationManager {

    @Resource
    private AgentService agentService;

    @Resource
    private AgentSessionService agentSessionService;

    @Resource
    private AgentSessionEventService agentSessionEventService;

    @Resource
    private AgentTaskService agentTaskService;

    @Resource
    private AgentVersionService agentVersionService;

    @Resource
    private SimpleAgentSupportManager simpleAgentSupportManager;

    @Resource
    private SimpleAgentRuntimeManager simpleAgentRuntimeManager;

    @Resource
    private SimpleAgentChatPersistenceManager simpleAgentChatPersistenceManager;

    @Resource
    private HookRuntimeManager hookRuntimeManager;

    @Resource
    private WebSocketPushService webSocketPushService;

    @Resource
    @Qualifier(CommonAsyncConfig.COMMON_ASYNC_EXECUTOR)
    private Executor executor;

    @Transactional(rollbackFor = Exception.class)
    @WebSocketPush(sessionId = "#p0.sessionId", sendResult = false, startEvent = "CHAT_START")
    public void chat(SimpleAgentChatRequest request) {
        validateChatRequest(request);
        AgentSession session = simpleAgentSupportManager.requireSession(request.getSessionId());
        validateSessionForChat(session, request.getAgentId());
        if (request.getLastReceivedEventSequence() != null) {
            replayMissedEvents(session, request.getLastReceivedEventSequence());
        }

        AgentTask task = simpleAgentChatPersistenceManager.createTask(session, request.getMessage(), null, 0);
        simpleAgentChatPersistenceManager.publishEvent(session, task, "USER_MESSAGE", task.getRequestMessage(), 1);
        CompletableFuture.runAsync(() -> executeTask(session.getSessionCode(), task.getTaskCode()), executor);
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentRecoverResponse recoverTask(String sessionCode, SimpleAgentRecoverRequest request) {
        AgentSession session = simpleAgentSupportManager.requireSession(sessionCode);
        AgentTask failedTask = resolveRecoverTask(session, request);

        AgentTask recoverTask = simpleAgentChatPersistenceManager.createTask(
                session,
                failedTask.getRequestMessage(),
                failedTask.getId(),
                resolveRetryCount(failedTask.getId())
        );
        simpleAgentChatPersistenceManager.publishEvent(session, recoverTask, "TASK_RECOVER", failedTask.getTaskCode(), 1);
        CompletableFuture.runAsync(() -> executeTask(session.getSessionCode(), recoverTask.getTaskCode()), executor);
        return SimpleAgentAssembler.toRecoverResponse(session, recoverTask, "任务恢复请求已受理");
    }

    private void executeTask(String sessionCode, String taskCode) {
        AgentSession session = agentSessionService.getOne(Wrappers.lambdaQuery(AgentSession.class)
                .eq(AgentSession::getSessionCode, sessionCode)
                .last("limit 1"));
        AgentTask task = agentTaskService.getOne(Wrappers.lambdaQuery(AgentTask.class)
                .eq(AgentTask::getTaskCode, taskCode)
                .last("limit 1"));
        if (session == null || task == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到会话或任务");
        }

        Agent agent = agentService.getById(session.getAgentId());
        AgentVersion version = agentVersionService.getById(session.getAgentVersionId());
        if (agent == null || version == null) {
            simpleAgentChatPersistenceManager.handleTaskError(session, task, "未找到智能体或版本信息");
            return;
        }

        ReactAgent reactAgent = simpleAgentRuntimeManager.getOrCreate(agent, version);
        SimpleAgentVersionConfigDTO versionConfig = simpleAgentSupportManager.parseConfig(version.getConfigSnapshotJson());
        List<String> selectedHookCodes = versionConfig == null ? null : versionConfig.getSelectedHookCodes();
        StringBuilder fullContent = new StringBuilder();
        try {
            HookRuntimeResultDTO preHookResult = hookRuntimeManager.applyPreModelHooks(
                    session.getTenantId(),
                    agent.getAgentCode(),
                    session.getSessionCode(),
                    selectedHookCodes,
                    task.getRequestMessage().trim()
            );
            if (Integer.valueOf(1).equals(preHookResult.getBlocked())) {
                simpleAgentChatPersistenceManager.handleTaskError(session, task, preHookResult.getFailureReason());
                return;
            }
            String runtimeRequestMessage = preHookResult.getContent();
            Flux<NodeOutput> stream = reactAgent.stream(runtimeRequestMessage);
            stream.doOnNext(output -> handleStreamingOutput(output, session, task, fullContent))
                    .doOnComplete(() -> handleTaskSuccessWithHooks(
                            session,
                            task,
                            agent.getAgentCode(),
                            selectedHookCodes,
                            runtimeRequestMessage,
                            fullContent.toString()
                    ))
                    .blockLast();
        } catch (Exception e) {
            simpleAgentChatPersistenceManager.handleTaskError(session, task, resolveErrorMessage(e));
        }
    }

    private void handleTaskSuccessWithHooks(
            AgentSession session,
            AgentTask task,
            String agentCode,
            List<String> selectedHookCodes,
            String runtimeRequestMessage,
            String rawResponseMessage
    ) {
        HookRuntimeResultDTO postHookResult = hookRuntimeManager.applyPostModelHooks(
                session.getTenantId(),
                agentCode,
                session.getSessionCode(),
                selectedHookCodes,
                runtimeRequestMessage,
                rawResponseMessage
        );
        if (Integer.valueOf(1).equals(postHookResult.getBlocked())) {
            simpleAgentChatPersistenceManager.handleTaskError(session, task, postHookResult.getFailureReason());
            return;
        }
        simpleAgentChatPersistenceManager.handleTaskSuccess(
                session,
                task,
                postHookResult.getContent()
        );
    }

    private void handleStreamingOutput(
            NodeOutput output,
            AgentSession session,
            AgentTask task,
            StringBuilder fullContent
    ) {
        if (!(output instanceof StreamingOutput streamingOutput)) {
            return;
        }
        OutputType outputType = streamingOutput.getOutputType();
        Message message = streamingOutput.message();

        if (outputType == OutputType.AGENT_MODEL_STREAMING && message instanceof AssistantMessage assistantMessage) {
            String reasoning = stringify(assistantMessage.getMetadata().get("reasoningContent"));
            if (StringUtils.hasText(reasoning)) {
                simpleAgentChatPersistenceManager.publishEvent(session, task, "AGENT_REASONING", reasoning, 1);
            }

            String text = assistantMessage.getText();
            if (StringUtils.hasText(text)) {
                fullContent.append(text);
                simpleAgentChatPersistenceManager.publishEvent(session, task, "AGENT_TOKEN", text, 1);
            }
            return;
        }

        if (outputType == OutputType.AGENT_TOOL_FINISHED && message instanceof ToolResponseMessage toolResponseMessage) {
            toolResponseMessage.getResponses().forEach(response ->
                    simpleAgentChatPersistenceManager.publishEvent(
                            session,
                            task,
                            "AGENT_TOOL",
                            response.name() + ": " + stringify(response.responseData()),
                            1
                    ));
        }
    }

    private void replayMissedEvents(AgentSession session, Long lastReceivedSequence) {
        agentSessionEventService.listReplayEvents(session.getId(), session.getTenantId(), lastReceivedSequence)
                .forEach(event -> webSocketPushService.sendToSession(
                        session.getSessionCode(),
                        event.getEventType(),
                        buildReplayEvent(session, event)
                ));
    }

    private void validateChatRequest(SimpleAgentChatRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "聊天请求不能为空");
        }
        if (!StringUtils.hasText(request.getSessionId())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "会话编号不能为空");
        }
        if (!StringUtils.hasText(request.getMessage())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "消息内容不能为空");
        }
    }

    private void validateSessionForChat(AgentSession session, String agentCode) {
        if (SimpleAgentConstants.SESSION_STATUS_CLOSED.equals(session.getSessionStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "当前会话已关闭");
        }
        if (StringUtils.hasText(agentCode) && !agentCode.equals(session.getAgentCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "当前会话与指定智能体不匹配");
        }
    }

    private AgentTask resolveRecoverTask(AgentSession session, SimpleAgentRecoverRequest request) {
        if (request != null && StringUtils.hasText(request.getTaskId())) {
            AgentTask task = simpleAgentSupportManager.requireTask(request.getTaskId());
            if (!task.getSessionId().equals(session.getId())) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                        "任务不属于当前会话");
            }
            if (!SimpleAgentConstants.TASK_STATUS_FAILED.equals(task.getTaskStatus())) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                        "仅允许恢复失败状态的任务");
            }
            return task;
        }

        AgentTask failedTask = agentTaskService.getLatestFailedTask(session.getId(), session.getTenantId());
        if (failedTask == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到失败任务");
        }
        return failedTask;
    }

    private SimpleAgentWsEvent buildReplayEvent(AgentSession session, AgentSessionEvent event) {
        return SimpleAgentAssembler.toWsEvent(
                session,
                resolveTaskCode(event.getTaskId()),
                event.getAgentVersionId(),
                session.getAgentVersionNo(),
                event.getEventType(),
                event.getEventBody(),
                event.getEventSequence(),
                event.getCreateTime() == null
                        ? System.currentTimeMillis()
                        : event.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        );
    }

    private String resolveTaskCode(Long taskId) {
        if (taskId == null) {
            return null;
        }
        AgentTask task = agentTaskService.getById(taskId);
        return task == null ? String.valueOf(taskId) : task.getTaskCode();
    }

    private Integer resolveRetryCount(Long sourceTaskId) {
        if (sourceTaskId == null) {
            return 0;
        }
        AgentTask sourceTask = agentTaskService.getById(sourceTaskId);
        if (sourceTask == null || sourceTask.getRetryCount() == null) {
            return 1;
        }
        return sourceTask.getRetryCount() + 1;
    }

    private String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "未知异常";
        }
        if (StringUtils.hasText(throwable.getMessage())) {
            return throwable.getMessage();
        }
        return throwable.getClass().getSimpleName();
    }
}
