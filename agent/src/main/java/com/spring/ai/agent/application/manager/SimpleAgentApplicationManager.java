package com.spring.ai.agent.application.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring.ai.agent.application.assmbler.SimpleAgentAssembler;
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
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Simple Agent 应用编排管理器。
 *
 * <p>负责 Agent 主档、版本、会话的创建与流转，不直接承担模型运行时执行职责。</p>
 */
@Component
public class SimpleAgentApplicationManager {

    @Resource
    private AgentService agentService;

    @Resource
    private AgentVersionService agentVersionService;

    @Resource
    private AgentSessionService agentSessionService;

    @Resource
    private AgentSessionEventService agentSessionEventService;

    @Resource
    private AgentTaskService agentTaskService;

    @Resource
    private SimpleAgentSupportManager simpleAgentSupportManager;

    public List<SimpleAgentSummaryResponse> listAgents() {
        Long tenantId = simpleAgentSupportManager.getCurrentTenantId();
        Long currentUserId = simpleAgentSupportManager.getCurrentUserId();
        return agentService.listByOwner(tenantId, currentUserId)
                .stream()
                .map(SimpleAgentAssembler::toSummaryResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentCreateResponse createAgent(SimpleAgentCreateRequest request) {
        validateCreateRequest(request);
        Long tenantId = simpleAgentSupportManager.getCurrentTenantId();
        Long currentUserId = simpleAgentSupportManager.getCurrentUserId();
        String currentUserName = simpleAgentSupportManager.getCurrentUserName();

        Agent agent = SimpleAgentAssembler.toCreateAgent(
                request,
                resolveAgentType(request.getAgentType()),
                tenantId,
                currentUserId,
                currentUserName
        );
        agentService.save(agent);

        AgentVersion version = createVersion(
                agent,
                request.getAgentName(),
                request.getDescription(),
                request.getSystemPrompt(),
                request.getSelectedCapabilities()
        );
        agent.setCurrentVersionId(version.getId());
        agent.setLatestVersionNo(version.getVersionNo());
        agentService.updateById(agent);
        return SimpleAgentAssembler.toCreateResponse(agent, version, request.getSelectedCapabilities());
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentCreateResponse updateAgent(String agentCode, SimpleAgentUpdateRequest request) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        validateUpdateRequest(request);

        AgentVersion version = createVersion(
                agent,
                request.getAgentName(),
                request.getDescription(),
                request.getSystemPrompt(),
                request.getSelectedCapabilities()
        );
        SimpleAgentAssembler.mergeAgentForUpdate(agent, request);
        agent.setCurrentVersionId(version.getId());
        agent.setLatestVersionNo(version.getVersionNo());
        if (SimpleAgentConstants.AGENT_STATUS_DISABLED.equals(agent.getAgentStatus())) {
            agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_DRAFT);
        }
        agentService.updateById(agent);
        return SimpleAgentAssembler.toCreateResponse(agent, version, request.getSelectedCapabilities());
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishAgent(String agentCode, Integer versionNo) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        Integer targetVersionNo = versionNo == null ? agent.getLatestVersionNo() : versionNo;
        AgentVersion version = simpleAgentSupportManager.requireAgentVersion(agent.getId(), targetVersionNo);

        agentVersionService.update(Wrappers.lambdaUpdate(AgentVersion.class)
                .eq(AgentVersion::getAgentId, agent.getId())
                .eq(AgentVersion::getTenantId, agent.getTenantId())
                .set(AgentVersion::getIsPublished, 0));
        version.setIsPublished(1);
        agentVersionService.updateById(version);

        agent.setPublishedVersionId(version.getId());
        agent.setPublishedVersionNo(version.getVersionNo());
        agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_PUBLISHED);
        agentService.updateById(agent);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableAgent(String agentCode) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_DISABLED);
        agentService.updateById(agent);
    }

    public SimpleAgentDetailResponse getAgentDetail(String agentCode) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        List<SimpleAgentVersionResponse> versions = agentVersionService.listByAgentId(agent.getId(), agent.getTenantId())
                .stream()
                .map(version -> SimpleAgentAssembler.toVersionResponse(
                        version,
                        simpleAgentSupportManager.parseCapabilities(version.getSelectedCapabilitiesJson())
                ))
                .toList();
        return SimpleAgentAssembler.toDetailResponse(agent, versions);
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentSessionResponse createSession(String agentCode, SimpleAgentSessionCreateRequest request) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        AgentVersion version = resolveSessionVersion(agent, request);
        AgentSession session = SimpleAgentAssembler.toCreateSession(agent, version);
        agentSessionService.save(session);
        return SimpleAgentAssembler.toSessionResponse(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentReconnectResponse reconnectSession(String sessionCode, SimpleAgentReconnectRequest request) {
        AgentSession session = simpleAgentSupportManager.requireSession(sessionCode);
        session.setConnectionStatus(SimpleAgentConstants.CONNECTION_STATUS_CONNECTED);
        session.setLastConnectedTime(LocalDateTime.now());
        agentSessionService.updateById(session);

        Long lastSequence = request == null ? null : request.getLastReceivedEventSequence();
        List<SimpleAgentWsEvent> missedEvents = agentSessionEventService
                .listReplayEvents(session.getId(), session.getTenantId(), lastSequence)
                .stream()
                .map(event -> buildReplayEvent(session, event))
                .toList();
        return SimpleAgentAssembler.toReconnectResponse(session, missedEvents);
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeSession(String sessionCode) {
        AgentSession session = simpleAgentSupportManager.requireSession(sessionCode);
        session.setSessionStatus(SimpleAgentConstants.SESSION_STATUS_CLOSED);
        session.setConnectionStatus(SimpleAgentConstants.CONNECTION_STATUS_DISCONNECTED);
        session.setLastDisconnectedTime(LocalDateTime.now());
        agentSessionService.updateById(session);
    }

    private AgentVersion createVersion(
            Agent agent,
            String agentName,
            String description,
            String systemPrompt,
            List<String> selectedCapabilities
    ) {
        int nextVersionNo = agent.getLatestVersionNo() == null ? 1 : agent.getLatestVersionNo() + 1;
        List<String> capabilities = SimpleAgentAssembler.normalizeCapabilities(selectedCapabilities);
        SimpleAgentVersionConfigDTO config = SimpleAgentAssembler.toVersionConfig(
                agentName,
                description,
                systemPrompt,
                capabilities
        );
        AgentVersion version = SimpleAgentAssembler.toCreateVersion(
                agent,
                nextVersionNo,
                config,
                simpleAgentSupportManager.toJson(capabilities),
                simpleAgentSupportManager.toJson(config)
        );
        agentVersionService.save(version);
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

    private AgentVersion resolveSessionVersion(Agent agent, SimpleAgentSessionCreateRequest request) {
        Integer versionNo = request == null ? null : request.getVersionNo();
        if (versionNo != null) {
            return simpleAgentSupportManager.requireAgentVersion(agent.getId(), versionNo);
        }
        if (agent.getPublishedVersionId() != null) {
            return simpleAgentSupportManager.requireAgentVersion(agent.getId(), agent.getPublishedVersionNo());
        }
        if (agent.getCurrentVersionId() != null) {
            AgentVersion currentVersion = agentVersionService.getById(agent.getCurrentVersionId());
            if (currentVersion != null) {
                return currentVersion;
            }
        }
        throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "agent version not found");
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
                SimpleAgentAssembler.toEpochMilli(event.getCreateTime())
        );
    }

    private String resolveTaskCode(Long taskId) {
        if (taskId == null) {
            return null;
        }
        AgentTask task = agentTaskService.getById(taskId);
        return task == null ? String.valueOf(taskId) : task.getTaskCode();
    }
}
