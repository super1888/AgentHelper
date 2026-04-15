package com.spring.ai.agent.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring.ai.common.constants.SimpleAgentConstants;
import com.spring.ai.agent.domain.dto.SimpleAgentVersionConfigDTO;
import com.spring.ai.agent.domain.request.SimpleAgentCreateRequest;
import com.spring.ai.agent.domain.request.SimpleAgentReconnectRequest;
import com.spring.ai.agent.domain.request.SimpleAgentSessionCreateRequest;
import com.spring.ai.agent.domain.request.SimpleAgentUpdateRequest;
import com.spring.ai.agent.domain.response.SimpleAgentCreateResponse;
import com.spring.ai.agent.domain.response.SimpleAgentDetailResponse;
import com.spring.ai.agent.domain.response.SimpleAgentReconnectResponse;
import com.spring.ai.agent.domain.response.SimpleAgentSessionResponse;
import com.spring.ai.agent.domain.response.SimpleAgentSummaryResponse;
import com.spring.ai.agent.domain.response.SimpleAgentVersionResponse;
import com.spring.ai.agent.domain.response.SimpleAgentWsEvent;
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
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Simple Agent 持久化服务。
 */
/**
 * Simple Agent 持久化服务。
 *
 * <p>负责 Agent 主档、版本、会话的增改查和发布流程，
 * 不直接执行模型推理。</p>
 */
@Service
public class SimpleAgentService {

    @Resource
    private SyAgentService syAgentService;

    @Resource
    private SyAgentVersionService syAgentVersionService;

    @Resource
    private SyAgentSessionService syAgentSessionService;

    @Resource
    private SyAgentSessionEventService syAgentSessionEventService;

    @Resource
    private SimpleAgentSupportService simpleAgentSupportService;

    @Resource
    private SyAgentTaskService syAgentTaskService;

    public List<SimpleAgentSummaryResponse> listAgents() {
        Long tenantId = simpleAgentSupportService.getCurrentTenantId();
        Long currentUserId = simpleAgentSupportService.getCurrentUserId();
        return syAgentService.listByOwner(tenantId, currentUserId)
                .stream()
                .map(this::buildSummaryResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentCreateResponse createAgent(SimpleAgentCreateRequest request) {
        validateCreateRequest(request);
        Long tenantId = simpleAgentSupportService.getCurrentTenantId();
        Long currentUserId = simpleAgentSupportService.getCurrentUserId();
        String currentUserName = simpleAgentSupportService.getCurrentUserName();

        // 先落主档，拿到主键后再创建首个版本快照。
        SyAgent agent = new SyAgent();
        agent.setAgentCode(UUID.randomUUID().toString());
        agent.setAgentName(request.getAgentName().trim());
        agent.setDescription(normalize(request.getDescription()));
        agent.setAgentType(resolveAgentType(request.getAgentType()));
        agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_DRAFT);
        agent.setTenantId(tenantId);
        agent.setOwnerUserId(currentUserId);
        agent.setOwnerUserName(currentUserName);
        agent.setLatestVersionNo(0);
        syAgentService.save(agent);

        SyAgentVersion version = createVersion(agent, request.getAgentName(), request.getDescription(),
                request.getSystemPrompt(), request.getSelectedCapabilities());
        agent.setCurrentVersionId(version.getId());
        agent.setLatestVersionNo(version.getVersionNo());
        syAgentService.updateById(agent);

        return buildCreateResponse(agent, version, request.getSelectedCapabilities());
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentCreateResponse updateAgent(String agentCode, SimpleAgentUpdateRequest request) {
        SyAgent agent = simpleAgentSupportService.requireAgent(agentCode);
        validateUpdateRequest(request);

        SyAgentVersion version = createVersion(agent, request.getAgentName(), request.getDescription(),
                request.getSystemPrompt(), request.getSelectedCapabilities());
        agent.setAgentName(request.getAgentName().trim());
        agent.setDescription(normalize(request.getDescription()));
        agent.setCurrentVersionId(version.getId());
        agent.setLatestVersionNo(version.getVersionNo());
        if (SimpleAgentConstants.AGENT_STATUS_DISABLED.equals(agent.getAgentStatus())) {
            agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_DRAFT);
        }
        syAgentService.updateById(agent);

        return buildCreateResponse(agent, version, request.getSelectedCapabilities());
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishAgent(String agentCode, Integer versionNo) {
        SyAgent agent = simpleAgentSupportService.requireAgent(agentCode);
        Integer targetVersionNo = versionNo == null ? agent.getLatestVersionNo() : versionNo;
        SyAgentVersion version = simpleAgentSupportService.requireAgentVersion(agent.getId(), targetVersionNo);

        // 同一个 Agent 同时只允许一个已发布版本。
        syAgentVersionService.update(Wrappers.lambdaUpdate(SyAgentVersion.class)
                .eq(SyAgentVersion::getAgentId, agent.getId())
                .eq(SyAgentVersion::getTenantId, agent.getTenantId())
                .set(SyAgentVersion::getIsPublished, 0));
        version.setIsPublished(1);
        syAgentVersionService.updateById(version);

        agent.setPublishedVersionId(version.getId());
        agent.setPublishedVersionNo(version.getVersionNo());
        agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_PUBLISHED);
        syAgentService.updateById(agent);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableAgent(String agentCode) {
        SyAgent agent = simpleAgentSupportService.requireAgent(agentCode);
        agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_DISABLED);
        syAgentService.updateById(agent);
    }

    public SimpleAgentDetailResponse getAgentDetail(String agentCode) {
        SyAgent agent = simpleAgentSupportService.requireAgent(agentCode);
        List<SimpleAgentVersionResponse> versions = syAgentVersionService.listByAgentId(agent.getId(), agent.getTenantId())
                .stream()
                .map(this::buildVersionResponse)
                .toList();

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

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentSessionResponse createSession(String agentCode, SimpleAgentSessionCreateRequest request) {
        SyAgent agent = simpleAgentSupportService.requireAgent(agentCode);
        SyAgentVersion version = resolveSessionVersion(agent, request);

        // 会话创建时绑定固定版本，保证后续行为可追溯。
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
        syAgentSessionService.save(session);
        return buildSessionResponse(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentReconnectResponse reconnectSession(String sessionCode, SimpleAgentReconnectRequest request) {
        SyAgentSession session = simpleAgentSupportService.requireSession(sessionCode);
        session.setConnectionStatus(SimpleAgentConstants.CONNECTION_STATUS_CONNECTED);
        session.setLastConnectedTime(LocalDateTime.now());
        syAgentSessionService.updateById(session);

        Long lastSequence = request == null ? null : request.getLastReceivedEventSequence();
        List<SimpleAgentWsEvent> missedEvents = syAgentSessionEventService.listReplayEvents(
                        session.getId(), session.getTenantId(), lastSequence)
                .stream()
                .map(this::buildReplayEvent)
                .toList();

        return SimpleAgentReconnectResponse.builder()
                .session(buildSessionResponse(session))
                .missedEvents(missedEvents)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeSession(String sessionCode) {
        SyAgentSession session = simpleAgentSupportService.requireSession(sessionCode);
        session.setSessionStatus(SimpleAgentConstants.SESSION_STATUS_CLOSED);
        session.setConnectionStatus(SimpleAgentConstants.CONNECTION_STATUS_DISCONNECTED);
        session.setLastDisconnectedTime(LocalDateTime.now());
        syAgentSessionService.updateById(session);
    }

    private SyAgentVersion createVersion(
            SyAgent agent,
            String agentName,
            String description,
            String systemPrompt,
            List<String> selectedCapabilities
    ) {
        // 版本号只增不改，保留完整配置变更轨迹。
        int nextVersionNo = agent.getLatestVersionNo() == null ? 1 : agent.getLatestVersionNo() + 1;
        List<String> capabilities = CollectionUtils.isEmpty(selectedCapabilities) ? List.of() : selectedCapabilities;
        SimpleAgentVersionConfigDTO config = SimpleAgentVersionConfigDTO.builder()
                .agentName(agentName.trim())
                .description(normalize(description))
                .systemPrompt(normalize(systemPrompt))
                .selectedCapabilities(capabilities)
                .build();

        SyAgentVersion version = new SyAgentVersion();
        version.setAgentId(agent.getId());
        version.setTenantId(agent.getTenantId());
        version.setVersionNo(nextVersionNo);
        version.setAgentName(agentName.trim());
        version.setDescription(normalize(description));
        version.setSystemPrompt(normalize(systemPrompt));
        version.setSelectedCapabilitiesJson(simpleAgentSupportService.toJson(capabilities));
        version.setConfigSnapshotJson(simpleAgentSupportService.toJson(config));
        version.setIsPublished(0);
        syAgentVersionService.save(version);
        return version;
    }

    private void validateCreateRequest(SimpleAgentCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getAgentName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "agentName must not be blank");
        }
    }

    private void validateUpdateRequest(SimpleAgentUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getAgentName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "agentName must not be blank");
        }
    }

    private String resolveAgentType(String agentType) {
        if (!StringUtils.hasText(agentType)) {
            return SimpleAgentConstants.AGENT_TYPE_REACT;
        }
        if (!SimpleAgentConstants.AGENT_TYPE_REACT.equalsIgnoreCase(agentType.trim())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "only REACT agent type is supported now");
        }
        return SimpleAgentConstants.AGENT_TYPE_REACT;
    }

    private SyAgentVersion resolveSessionVersion(SyAgent agent, SimpleAgentSessionCreateRequest request) {
        Integer versionNo = request == null ? null : request.getVersionNo();
        if (versionNo != null) {
            return simpleAgentSupportService.requireAgentVersion(agent.getId(), versionNo);
        }
        // 默认优先使用已发布版本，让新会话绑定稳定配置。
        if (agent.getPublishedVersionId() != null) {
            return simpleAgentSupportService.requireAgentVersion(agent.getId(), agent.getPublishedVersionNo());
        }
        if (agent.getCurrentVersionId() != null) {
            return syAgentVersionService.getById(agent.getCurrentVersionId());
        }
        throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "agent version not found");
    }

    private SimpleAgentCreateResponse buildCreateResponse(SyAgent agent, SyAgentVersion version, List<String> capabilities) {
        List<String> selectedCapabilities = capabilities == null ? List.of() : capabilities;
        return SimpleAgentCreateResponse.builder()
                .agentId(agent.getAgentCode())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .selectedCapabilities(selectedCapabilities)
                .currentVersionNo(version.getVersionNo())
                .publishedVersionNo(agent.getPublishedVersionNo())
                .websocketEndpoint("/ws")
                .websocketTopic("/topic/session/{sessionId}")
                .websocketSendDestination("/app/agent/chat")
                .build();
    }

    private SimpleAgentVersionResponse buildVersionResponse(SyAgentVersion version) {
        return SimpleAgentVersionResponse.builder()
                .versionId(version.getId())
                .versionNo(version.getVersionNo())
                .agentName(version.getAgentName())
                .description(version.getDescription())
                .systemPrompt(version.getSystemPrompt())
                .selectedCapabilities(simpleAgentSupportService.parseCapabilities(version.getSelectedCapabilitiesJson()))
                .published(version.getIsPublished() != null && version.getIsPublished() == 1)
                .createTime(toEpochMilli(version.getCreateTime()))
                .build();
    }

    private SimpleAgentSummaryResponse buildSummaryResponse(SyAgent agent) {
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

    private SimpleAgentSessionResponse buildSessionResponse(SyAgentSession session) {
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

    private SimpleAgentWsEvent buildReplayEvent(SyAgentSessionEvent event) {
        SyAgentSession session = syAgentSessionService.getById(event.getSessionId());
        return SimpleAgentWsEvent.builder()
                .agentId(session == null ? String.valueOf(event.getAgentId()) : session.getAgentCode())
                .sessionId(event.getSessionCode())
                .taskId(resolveTaskCode(event.getTaskId()))
                .agentVersionId(event.getAgentVersionId())
                .agentVersionNo(session == null ? null : session.getAgentVersionNo())
                .eventSequence(event.getEventSequence())
                .event(event.getEventType())
                .data(event.getEventBody())
                .timestamp(toEpochMilli(event.getCreateTime()))
                .build();
    }

    private Long toEpochMilli(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveTaskCode(Long taskId) {
        if (taskId == null) {
            return null;
        }
        SyAgentTask task = syAgentTaskService.getById(taskId);
        return task == null ? String.valueOf(taskId) : task.getTaskCode();
    }
}
