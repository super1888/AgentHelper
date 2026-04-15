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
    private SyAgentService syAgentService;

    @Resource
    private SyAgentVersionService syAgentVersionService;

    @Resource
    private SyAgentSessionService syAgentSessionService;

    @Resource
    private SyAgentSessionEventService syAgentSessionEventService;

    @Resource
    private SyAgentTaskService syAgentTaskService;

    @Resource
    private SimpleAgentSupportManager simpleAgentSupportManager;

    public List<SimpleAgentSummaryResponse> listAgents() {
        Long tenantId = simpleAgentSupportManager.getCurrentTenantId();
        Long currentUserId = simpleAgentSupportManager.getCurrentUserId();
        return syAgentService.listByOwner(tenantId, currentUserId)
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

        SyAgent agent = SimpleAgentAssembler.toCreateAgent(
                request,
                resolveAgentType(request.getAgentType()),
                tenantId,
                currentUserId,
                currentUserName
        );
        syAgentService.save(agent);

        SyAgentVersion version = createVersion(
                agent,
                request.getAgentName(),
                request.getDescription(),
                request.getSystemPrompt(),
                request.getSelectedCapabilities()
        );
        agent.setCurrentVersionId(version.getId());
        agent.setLatestVersionNo(version.getVersionNo());
        syAgentService.updateById(agent);
        return SimpleAgentAssembler.toCreateResponse(agent, version, request.getSelectedCapabilities());
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentCreateResponse updateAgent(String agentCode, SimpleAgentUpdateRequest request) {
        SyAgent agent = simpleAgentSupportManager.requireAgent(agentCode);
        validateUpdateRequest(request);

        SyAgentVersion version = createVersion(
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
        syAgentService.updateById(agent);
        return SimpleAgentAssembler.toCreateResponse(agent, version, request.getSelectedCapabilities());
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishAgent(String agentCode, Integer versionNo) {
        SyAgent agent = simpleAgentSupportManager.requireAgent(agentCode);
        Integer targetVersionNo = versionNo == null ? agent.getLatestVersionNo() : versionNo;
        SyAgentVersion version = simpleAgentSupportManager.requireAgentVersion(agent.getId(), targetVersionNo);

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
        SyAgent agent = simpleAgentSupportManager.requireAgent(agentCode);
        agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_DISABLED);
        syAgentService.updateById(agent);
    }

    public SimpleAgentDetailResponse getAgentDetail(String agentCode) {
        SyAgent agent = simpleAgentSupportManager.requireAgent(agentCode);
        List<SimpleAgentVersionResponse> versions = syAgentVersionService.listByAgentId(agent.getId(), agent.getTenantId())
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
        SyAgent agent = simpleAgentSupportManager.requireAgent(agentCode);
        SyAgentVersion version = resolveSessionVersion(agent, request);
        SyAgentSession session = SimpleAgentAssembler.toCreateSession(agent, version);
        syAgentSessionService.save(session);
        return SimpleAgentAssembler.toSessionResponse(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentReconnectResponse reconnectSession(String sessionCode, SimpleAgentReconnectRequest request) {
        SyAgentSession session = simpleAgentSupportManager.requireSession(sessionCode);
        session.setConnectionStatus(SimpleAgentConstants.CONNECTION_STATUS_CONNECTED);
        session.setLastConnectedTime(LocalDateTime.now());
        syAgentSessionService.updateById(session);

        Long lastSequence = request == null ? null : request.getLastReceivedEventSequence();
        List<SimpleAgentWsEvent> missedEvents = syAgentSessionEventService
                .listReplayEvents(session.getId(), session.getTenantId(), lastSequence)
                .stream()
                .map(event -> buildReplayEvent(session, event))
                .toList();
        return SimpleAgentAssembler.toReconnectResponse(session, missedEvents);
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeSession(String sessionCode) {
        SyAgentSession session = simpleAgentSupportManager.requireSession(sessionCode);
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
        int nextVersionNo = agent.getLatestVersionNo() == null ? 1 : agent.getLatestVersionNo() + 1;
        List<String> capabilities = SimpleAgentAssembler.normalizeCapabilities(selectedCapabilities);
        SimpleAgentVersionConfigDTO config = SimpleAgentAssembler.toVersionConfig(
                agentName,
                description,
                systemPrompt,
                capabilities
        );
        SyAgentVersion version = SimpleAgentAssembler.toCreateVersion(
                agent,
                nextVersionNo,
                config,
                simpleAgentSupportManager.toJson(capabilities),
                simpleAgentSupportManager.toJson(config)
        );
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
            return simpleAgentSupportManager.requireAgentVersion(agent.getId(), versionNo);
        }
        if (agent.getPublishedVersionId() != null) {
            return simpleAgentSupportManager.requireAgentVersion(agent.getId(), agent.getPublishedVersionNo());
        }
        if (agent.getCurrentVersionId() != null) {
            SyAgentVersion currentVersion = syAgentVersionService.getById(agent.getCurrentVersionId());
            if (currentVersion != null) {
                return currentVersion;
            }
        }
        throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "agent version not found");
    }

    private SimpleAgentWsEvent buildReplayEvent(SyAgentSession session, SyAgentSessionEvent event) {
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
        SyAgentTask task = syAgentTaskService.getById(taskId);
        return task == null ? String.valueOf(taskId) : task.getTaskCode();
    }
}
