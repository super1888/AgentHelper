package com.spring.ai.agent.application.assmbler;

import com.spring.ai.agent.domain.dto.SimpleAgentVersionConfigDTO;
import com.spring.ai.agent.domain.dto.AgentPromptTemplateVariableDTO;
import com.spring.ai.agent.domain.dto.SimpleAgentModelBindingDTO;
import com.spring.ai.agent.domain.dto.SimpleAgentPromptConfigDTO;
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
import com.spring.ai.common.repository.enitiy.Agent;
import com.spring.ai.common.repository.enitiy.AgentSession;
import com.spring.ai.common.repository.enitiy.AgentSessionEvent;
import com.spring.ai.common.repository.enitiy.AgentTask;
import com.spring.ai.common.repository.enitiy.AgentVersion;
import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
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

    public static Agent toCreateAgent(
            SimpleAgentCreateRequest request,
            String agentType,
            Long tenantId,
            Long currentUserId,
            String currentUserName
    ) {
        Agent agent = new Agent();
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

    public static void mergeAgentForUpdate(Agent agent, SimpleAgentUpdateRequest request) {
        agent.setAgentName(trim(request.getAgentName()));
        agent.setDescription(trimToNull(request.getDescription()));
    }

    public static SimpleAgentVersionConfigDTO toVersionConfig(
            String agentName,
            String description,
            String systemPrompt,
            List<String> selectedCapabilities,
            List<String> selectedHookCodes,
            SimpleAgentPromptConfigDTO promptConfig,
            SimpleAgentModelBindingDTO modelBinding
    ) {
        return SimpleAgentVersionConfigDTO.builder()
                .agentName(trim(agentName))
                .description(trimToNull(description))
                .systemPrompt(trimToNull(systemPrompt))
                .selectedCapabilities(normalizeCapabilities(selectedCapabilities))
                .selectedHookCodes(normalizeHookCodes(selectedHookCodes))
                .promptConfig(promptConfig)
                .modelBinding(modelBinding)
                .build();
    }

    public static AgentVersion toCreateVersion(
            Agent agent,
            Integer nextVersionNo,
            SimpleAgentVersionConfigDTO config,
            String selectedCapabilitiesJson,
            String configSnapshotJson
    ) {
        AgentVersion version = new AgentVersion();
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

    public static AgentSession toCreateSession(Agent agent, AgentVersion version) {
        AgentSession session = new AgentSession();
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

    public static AgentTask toCreateTask(
            AgentSession session,
            String requestMessage,
            Long sourceTaskId,
            Integer retryCount
    ) {
        AgentTask task = new AgentTask();
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

    public static AgentSessionEvent toCreateSessionEvent(
            AgentSession session,
            Long taskId,
            String eventType,
            String eventBody,
            Integer replayable,
            Long eventSequence
    ) {
        AgentSessionEvent event = new AgentSessionEvent();
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
            Agent agent,
            AgentVersion version,
            List<String> selectedCapabilities,
            List<String> selectedHookCodes
    ) {
        return SimpleAgentCreateResponse.builder()
                .agentId(agent.getAgentCode())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .selectedCapabilities(normalizeCapabilities(selectedCapabilities))
                .selectedHookCodes(normalizeHookCodes(selectedHookCodes))
                .currentVersionNo(version.getVersionNo())
                .publishedVersionNo(agent.getPublishedVersionNo())
                .websocketEndpoint("/ws")
                .websocketTopic("/topic/session/{sessionId}")
                .websocketSendDestination("/app/agent/chat")
                .build();
    }

    public static SimpleAgentDetailResponse toDetailResponse(
            Agent agent,
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

    public static SimpleAgentSummaryResponse toSummaryResponse(Agent agent) {
        return toSummaryResponse(agent, null);
    }

    public static SimpleAgentSummaryResponse toSummaryResponse(Agent agent, SimpleAgentVersionConfigDTO config) {
        SimpleAgentModelBindingDTO modelBinding = config == null ? null : config.getModelBinding();
        return SimpleAgentSummaryResponse.builder()
                .agentId(agent.getAgentCode())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .agentType(agent.getAgentType())
                .agentStatus(agent.getAgentStatus())
                .currentVersionNo(agent.getLatestVersionNo())
                .publishedVersionNo(agent.getPublishedVersionNo())
                .ownerUserName(agent.getOwnerUserName())
                .modelCode(modelBinding == null ? null : modelBinding.getModelCode())
                .modelName(modelBinding == null ? null : modelBinding.getModelName())
                .providerName(modelBinding == null ? null : modelBinding.getProviderName())
                .build();
    }

    public static SimpleAgentVersionResponse toVersionResponse(
            AgentVersion version,
            List<String> selectedCapabilities,
            SimpleAgentVersionConfigDTO config
    ) {
        SimpleAgentPromptConfigDTO promptConfig = config == null ? null : config.getPromptConfig();
        SimpleAgentModelBindingDTO modelBinding = config == null ? null : config.getModelBinding();
        return SimpleAgentVersionResponse.builder()
                .versionId(version.getId() == null ? null : String.valueOf(version.getId()))
                .versionNo(version.getVersionNo())
                .agentName(version.getAgentName())
                .description(version.getDescription())
                .systemPrompt(version.getSystemPrompt())
                .selectedCapabilities(normalizeCapabilities(selectedCapabilities))
                .selectedHookCodes(normalizeHookCodes(config == null ? null : config.getSelectedHookCodes()))
                .promptTemplateId(promptConfig == null ? null : promptConfig.getPromptTemplateId())
                .promptTemplateCode(promptConfig == null ? null : promptConfig.getPromptTemplateCode())
                .promptTemplateName(promptConfig == null ? null : promptConfig.getPromptTemplateName())
                .promptBindingType(promptConfig == null ? null : promptConfig.getPromptBindingType())
                .promptSourceType(promptConfig == null ? null : promptConfig.getPromptSourceType())
                .promptTemplatePath(promptConfig == null ? null : promptConfig.getPromptTemplatePath())
                .promptTemplateContent(promptConfig == null ? null : promptConfig.getPromptTemplateContent())
                .promptVariableDefinitions(promptConfig == null ? null : promptConfig.getPromptVariableDefinitions())
                .promptVariables(promptConfig == null ? null : promptConfig.getPromptVariables())
                .modelCode(modelBinding == null ? null : modelBinding.getModelCode())
                .modelName(modelBinding == null ? null : modelBinding.getModelName())
                .providerConfigCode(modelBinding == null ? null : modelBinding.getProviderConfigCode())
                .providerEnum(modelBinding == null ? null : modelBinding.getProviderEnum())
                .providerName(modelBinding == null ? null : modelBinding.getProviderName())
                .modelIdentifier(modelBinding == null ? null : modelBinding.getModelIdentifier())
                .modelType(modelBinding == null ? null : modelBinding.getModelType())
                .published(version.getIsPublished() != null && version.getIsPublished() == 1)
                .createTime(toEpochMilli(version.getCreateTime()))
                .build();
    }

    public static SimpleAgentSessionResponse toSessionResponse(AgentSession session) {
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
            AgentSession session,
            List<SimpleAgentWsEvent> missedEvents
    ) {
        return SimpleAgentReconnectResponse.builder()
                .session(toSessionResponse(session))
                .missedEvents(missedEvents)
                .build();
    }

    public static SimpleAgentRecoverResponse toRecoverResponse(
            AgentSession session,
            AgentTask task,
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
            AgentSession session,
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

    public static List<String> normalizeHookCodes(List<String> selectedHookCodes) {
        if (CollectionUtils.isEmpty(selectedHookCodes)) {
            return List.of();
        }
        return selectedHookCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    public static List<AgentPromptTemplateVariableDTO> toPromptVariableDefinitions(
            List<PromptTemplateVariableDTO> variableDefinitions
    ) {
        if (CollectionUtils.isEmpty(variableDefinitions)) {
            return List.of();
        }
        return variableDefinitions.stream()
                .map(item -> AgentPromptTemplateVariableDTO.builder()
                        .variableName(item.getVariableName())
                        .required(item.getRequired())
                        .defaultValue(item.getDefaultValue())
                        .description(item.getDescription())
                        .build())
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
