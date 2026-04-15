package com.spring.ai.agent.application.assmbler;

import com.spring.ai.agent.domain.dto.SimpleAgentVersionConfigDTO;
import com.spring.ai.agent.domain.request.SimpleAgentCreateRequest;
import com.spring.ai.agent.domain.request.SimpleAgentUpdateRequest;
import com.spring.ai.agent.domain.response.SimpleAgentCreateResponse;
import com.spring.ai.agent.domain.response.SimpleAgentDetailResponse;
import com.spring.ai.agent.domain.response.SimpleAgentRecoverResponse;
import com.spring.ai.agent.domain.response.SimpleAgentReconnectResponse;
import com.spring.ai.agent.domain.response.SimpleAgentSessionResponse;
import com.spring.ai.agent.domain.response.SimpleAgentSummaryResponse;
import com.spring.ai.agent.domain.response.SimpleAgentVersionResponse;
import com.spring.ai.agent.domain.response.SimpleAgentWsEvent;
import com.spring.ai.common.constants.SimpleAgentConstants;
import com.spring.ai.common.repository.enitiy.SyAgent;
import com.spring.ai.common.repository.enitiy.SyAgentSession;
import com.spring.ai.common.repository.enitiy.SyAgentSessionEvent;
import com.spring.ai.common.repository.enitiy.SyAgentTask;
import com.spring.ai.common.repository.enitiy.SyAgentVersion;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Simple Agent 应用层对象组装器。
 *
 * <p>负责集中处理 Agent 领域中的实体构造、版本快照组装以及接口响应转换，
 * 避免编排类中混入大量样板式字段复制逻辑。</p>
 */
public final class SimpleAgentAssembler {

    private SimpleAgentAssembler() {
    }

    public static SyAgent toCreateAgent(
            SimpleAgentCreateRequest request,
            String agentType,
            Long tenantId,
            Long currentUserId,
            String currentUserName
    ) {
        SyAgent agent = new SyAgent();
        agent.setAgentCode(UUID.randomUUID().toString());
        agent.setAgentName(trim(request.getAgentName()));
        agent.setDescription(trimToNull(request.getDescription()));
        agent.setAgentType(agentType);
        agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_DRAFT);
        agent.setTenantId(tenantId);
        agent.setOwnerUserId(currentUserId);
        agent.setOwnerUserName(currentUserName);
        agent.setLatestVersionNo(0);
        return agent;
    }

    public static void mergeAgentForUpdate(SyAgent agent, SimpleAgentUpdateRequest request) {
        agent.setAgentName(trim(request.getAgentName()));
        agent.setDescription(trimToNull(request.getDescription()));
    }

    public static SimpleAgentVersionConfigDTO toVersionConfig(
            String agentName,
            String description,
            String systemPrompt,
            List<String> selectedCapabilities
    ) {
        return SimpleAgentVersionConfigDTO.builder()
                .agentName(trim(agentName))
                .description(trimToNull(description))
                .systemPrompt(trimToNull(systemPrompt))
                .selectedCapabilities(normalizeCapabilities(selectedCapabilities))
                .build();
    }

    public static SyAgentVersion toCreateVersion(
            SyAgent agent,
            Integer nextVersionNo,
            SimpleAgentVersionConfigDTO config,
            String selectedCapabilitiesJson,
            String configSnapshotJson
    ) {
        SyAgentVersion version = new SyAgentVersion();
        version.setAgentId(agent.getId());
        version.setTenantId(agent.getTenantId());
        version.setVersionNo(nextVersionNo);
        version.setAgentName(config.getAgentName());
        version.setDescription(config.getDescription());
        version.setSystemPrompt(config.getSystemPrompt());
        version.setSelectedCapabilitiesJson(selectedCapabilitiesJson);
        version.setConfigSnapshotJson(configSnapshotJson);
        version.setIsPublished(0);
        return version;
    }

    public static SyAgentSession toCreateSession(SyAgent agent, SyAgentVersion version) {
        SyAgentSession session = new SyAgentSession();
        session.setSessionCode(UUID.randomUUID().toString());
        session.setAgentId(agent.getId());
        session.setAgentCode(agent.getAgentCode());
        session.setAgentVersionId(version.getId());
        session.setAgentVersionNo(version.getVersionNo());
        session.setTenantId(agent.getTenantId());
        session.setOwnerUserId(agent.getOwnerUserId());
        session.setOwnerUserName(agent.getOwnerUserName());
        session.setSessionStatus(SimpleAgentConstants.SESSION_STATUS_ACTIVE);
        session.setConnectionStatus(SimpleAgentConstants.CONNECTION_STATUS_CONNECTED);
        session.setLastEventSequence(0L);
        session.setLastConnectedTime(LocalDateTime.now());
        return session;
    }

    public static SyAgentTask toCreateTask(
            SyAgentSession session,
            String requestMessage,
            Long sourceTaskId,
            Integer retryCount
    ) {
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
        task.setRequestMessage(trim(requestMessage));
        task.setRetryCount(retryCount);
        return task;
    }

    public static SyAgentSessionEvent toCreateSessionEvent(
            SyAgentSession session,
            Long taskId,
            String eventType,
            String eventBody,
            Integer replayable,
            Long eventSequence
    ) {
        SyAgentSessionEvent event = new SyAgentSessionEvent();
        event.setSessionId(session.getId());
        event.setSessionCode(session.getSessionCode());
        event.setAgentId(session.getAgentId());
        event.setAgentVersionId(session.getAgentVersionId());
        event.setTenantId(session.getTenantId());
        event.setTaskId(taskId);
        event.setEventSequence(eventSequence);
        event.setEventType(eventType);
        event.setEventBody(eventBody);
        event.setReplayable(replayable);
        return event;
    }

    public static SimpleAgentCreateResponse toCreateResponse(
            SyAgent agent,
            SyAgentVersion version,
            List<String> selectedCapabilities
    ) {
        return SimpleAgentCreateResponse.builder()
                .agentId(agent.getAgentCode())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .selectedCapabilities(normalizeCapabilities(selectedCapabilities))
                .currentVersionNo(version.getVersionNo())
                .publishedVersionNo(agent.getPublishedVersionNo())
                .websocketEndpoint("/ws")
                .websocketTopic("/topic/session/{sessionId}")
                .websocketSendDestination("/app/agent/chat")
                .build();
    }

    public static SimpleAgentDetailResponse toDetailResponse(
            SyAgent agent,
            List<SimpleAgentVersionResponse> versions
    ) {
        return SimpleAgentDetailResponse.builder()
                .agentId(agent.getAgentCode())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .agentType(agent.getAgentType())
                .agentStatus(agent.getAgentStatus())
                .currentVersionNo(agent.getLatestVersionNo())
                .publishedVersionNo(agent.getPublishedVersionNo())
                .ownerUserId(agent.getOwnerUserId())
                .ownerUserName(agent.getOwnerUserName())
                .versions(versions)
                .build();
    }

    public static SimpleAgentSummaryResponse toSummaryResponse(SyAgent agent) {
        return SimpleAgentSummaryResponse.builder()
                .agentId(agent.getAgentCode())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .agentType(agent.getAgentType())
                .agentStatus(agent.getAgentStatus())
                .currentVersionNo(agent.getLatestVersionNo())
                .publishedVersionNo(agent.getPublishedVersionNo())
                .ownerUserName(agent.getOwnerUserName())
                .build();
    }

    public static SimpleAgentVersionResponse toVersionResponse(
            SyAgentVersion version,
            List<String> selectedCapabilities
    ) {
        return SimpleAgentVersionResponse.builder()
                .versionId(version.getId())
                .versionNo(version.getVersionNo())
                .agentName(version.getAgentName())
                .description(version.getDescription())
                .systemPrompt(version.getSystemPrompt())
                .selectedCapabilities(normalizeCapabilities(selectedCapabilities))
                .published(version.getIsPublished() != null && version.getIsPublished() == 1)
                .createTime(toEpochMilli(version.getCreateTime()))
                .build();
    }

    public static SimpleAgentSessionResponse toSessionResponse(SyAgentSession session) {
        return SimpleAgentSessionResponse.builder()
                .sessionId(session.getSessionCode())
                .agentId(session.getAgentCode())
                .agentVersionNo(session.getAgentVersionNo())
                .agentVersionId(session.getAgentVersionId())
                .sessionStatus(session.getSessionStatus())
                .connectionStatus(session.getConnectionStatus())
                .lastEventSequence(session.getLastEventSequence())
                .websocketEndpoint("/ws")
                .websocketTopic("/topic/session/{sessionId}")
                .websocketSendDestination("/app/agent/chat")
                .build();
    }

    public static SimpleAgentReconnectResponse toReconnectResponse(
            SyAgentSession session,
            List<SimpleAgentWsEvent> missedEvents
    ) {
        return SimpleAgentReconnectResponse.builder()
                .session(toSessionResponse(session))
                .missedEvents(missedEvents)
                .build();
    }

    public static SimpleAgentRecoverResponse toRecoverResponse(
            SyAgentSession session,
            SyAgentTask task,
            String message
    ) {
        return SimpleAgentRecoverResponse.builder()
                .sessionId(session.getSessionCode())
                .taskId(task.getTaskCode())
                .taskStatus(task.getTaskStatus())
                .message(message)
                .build();
    }

    public static SimpleAgentWsEvent toWsEvent(
            SyAgentSession session,
            String taskCode,
            Long agentVersionId,
            Integer agentVersionNo,
            String event,
            Object data,
            Long eventSequence,
            Long timestamp
    ) {
        return SimpleAgentWsEvent.builder()
                .agentId(session.getAgentCode())
                .sessionId(session.getSessionCode())
                .taskId(taskCode)
                .agentVersionId(agentVersionId)
                .agentVersionNo(agentVersionNo)
                .eventSequence(eventSequence)
                .event(event)
                .data(data)
                .timestamp(timestamp)
                .build();
    }

    public static Long toEpochMilli(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static List<String> normalizeCapabilities(List<String> selectedCapabilities) {
        if (CollectionUtils.isEmpty(selectedCapabilities)) {
            return List.of();
        }
        return selectedCapabilities.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
