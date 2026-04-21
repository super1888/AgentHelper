package com.spring.ai.agent.application.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring.ai.agent.application.assmbler.SimpleAgentAssembler;
import com.spring.ai.agent.domain.dto.AgentPromptTemplateVariableDTO;
import com.spring.ai.agent.domain.dto.SimpleAgentVersionConfigDTO;
import com.spring.ai.agent.domain.dto.SimpleAgentPromptConfigDTO;
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
import com.spring.ai.prompt.application.manager.PromptTemplateResolver;
import com.spring.ai.prompt.config.PromptTemplateConstants;
import com.spring.ai.prompt.domain.dto.PromptTemplateBindDTO;
import com.spring.ai.prompt.domain.dto.PromptTemplateResolvedDTO;
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

    // 注入所需的Service组件
    @Resource
    private AgentService agentService; // Agent主档服务

    @Resource
    private AgentVersionService agentVersionService; // Agent版本服务

    @Resource
    private AgentSessionService agentSessionService; // Agent会话服务

    @Resource
    private AgentSessionEventService agentSessionEventService; // Agent会话事件服务

    @Resource
    private AgentTaskService agentTaskService; // Agent任务服务

    @Resource
    private SimpleAgentSupportManager simpleAgentSupportManager; // SimpleAgent支持管理器

    @Resource
    private PromptTemplateResolver promptTemplateResolver; // 提示词模板解析器

    /**
     * 获取当前用户的所有Agent列表
     *
     * @return Agent摘要列表
     */
    public List<SimpleAgentSummaryResponse> listAgents() {
        Long tenantId = simpleAgentSupportManager.getCurrentTenantId();
        Long currentUserId = simpleAgentSupportManager.getCurrentUserId();
        return agentService.listByOwner(tenantId, currentUserId)
                .stream()
                .map(SimpleAgentAssembler::toSummaryResponse)
                .toList();
    }

    /**
     * 创建新的Agent
     *
     * @param request 创建Agent请求
     * @return 创建响应
     */
    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentCreateResponse createAgent(SimpleAgentCreateRequest request) {
        validateCreateRequest(request);
        Long tenantId = simpleAgentSupportManager.getCurrentTenantId();
        Long currentUserId = simpleAgentSupportManager.getCurrentUserId();
        String currentUserName = simpleAgentSupportManager.getCurrentUserName();

        // 转换为Agent实体并保存
        Agent agent = SimpleAgentAssembler.toCreateAgent(
                request,
                resolveAgentType(request.getAgentType()),
                tenantId,
                currentUserId,
                currentUserName
        );
        agentService.save(agent);

        // 创建初始版本
        AgentVersion version = createVersion(
                agent,
                request.getAgentName(),
                request.getDescription(),
                request.getSystemPrompt(),
                request.getSelectedCapabilities(),
                request.getSelectedHookCodes(),
                request.getPromptConfig()
        );
        agent.setCurrentVersionId(version.getId());
        agent.setLatestVersionNo(version.getVersionNo());
        agentService.updateById(agent);
        return SimpleAgentAssembler.toCreateResponse(
                agent,
                version,
                request.getSelectedCapabilities(),
                request.getSelectedHookCodes()
        );
    }

    /**
     * 更新Agent信息
     *
     * @param agentCode Agent编码
     * @param request 更新请求
     * @return 更新响应
     */
    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentCreateResponse updateAgent(String agentCode, SimpleAgentUpdateRequest request) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        validateUpdateRequest(request);

        // 创建新版本并更新Agent信息
        AgentVersion version = createVersion(
                agent,
                request.getAgentName(),
                request.getDescription(),
                request.getSystemPrompt(),
                request.getSelectedCapabilities(),
                request.getSelectedHookCodes(),
                request.getPromptConfig()
        );
        SimpleAgentAssembler.mergeAgentForUpdate(agent, request);
        agent.setCurrentVersionId(version.getId());
        agent.setLatestVersionNo(version.getVersionNo());
        if (SimpleAgentConstants.AGENT_STATUS_DISABLED.equals(agent.getAgentStatus())) {
            agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_DRAFT);
        }
        agentService.updateById(agent);
        return SimpleAgentAssembler.toCreateResponse(
                agent,
                version,
                request.getSelectedCapabilities(),
                request.getSelectedHookCodes()
        );
    }

    /**
     * 发布Agent
     *
     * @param agentCode Agent编码
     * @param versionNo 版本号，为空则使用最新版本
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishAgent(String agentCode, Integer versionNo) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        Integer targetVersionNo = versionNo == null ? agent.getLatestVersionNo() : versionNo;
        AgentVersion version = simpleAgentSupportManager.requireAgentVersion(agent.getId(), targetVersionNo);

        // 取消其他版本的发布状态，设置当前版本为已发布
        agentVersionService.update(Wrappers.lambdaUpdate(AgentVersion.class)
                .eq(AgentVersion::getAgentId, agent.getId())
                .eq(AgentVersion::getTenantId, agent.getTenantId())
                .set(AgentVersion::getIsPublished, 0));
        version.setIsPublished(1);
        agentVersionService.updateById(version);

        // 更新Agent的发布版本信息
        agent.setPublishedVersionId(version.getId());
        agent.setPublishedVersionNo(version.getVersionNo());
        agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_PUBLISHED);
        agentService.updateById(agent);
    }

    /**
     * 禁用Agent
     *
     * @param agentCode Agent编码
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableAgent(String agentCode) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_DISABLED);
        agentService.updateById(agent);
    }

    /**
     * 删除已禁用的 Agent，并级联清理其版本、会话、任务与事件数据。
     *
     * @param agentCode Agent 编码
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(String agentCode) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        validateAgentCanDelete(agent);

        // 先删事件和任务，再删会话与版本，最后删除 Agent 主档，避免留下悬挂数据。
        List<Long> sessionIds = agentSessionService.list(Wrappers.lambdaQuery(AgentSession.class)
                        .eq(AgentSession::getAgentId, agent.getId())
                        .eq(AgentSession::getTenantId, agent.getTenantId()))
                .stream()
                .map(AgentSession::getId)
                .toList();

        if (!sessionIds.isEmpty()) {
            agentSessionEventService.remove(Wrappers.lambdaQuery(AgentSessionEvent.class)
                    .in(AgentSessionEvent::getSessionId, sessionIds)
                    .eq(AgentSessionEvent::getTenantId, agent.getTenantId()));
            agentTaskService.remove(Wrappers.lambdaQuery(AgentTask.class)
                    .in(AgentTask::getSessionId, sessionIds)
                    .eq(AgentTask::getTenantId, agent.getTenantId()));
            agentSessionService.remove(Wrappers.lambdaQuery(AgentSession.class)
                    .in(AgentSession::getId, sessionIds)
                    .eq(AgentSession::getTenantId, agent.getTenantId()));
        }

        agentVersionService.remove(Wrappers.lambdaQuery(AgentVersion.class)
                .eq(AgentVersion::getAgentId, agent.getId())
                .eq(AgentVersion::getTenantId, agent.getTenantId()));
        agentService.removeById(agent.getId());
    }

    public SimpleAgentDetailResponse getAgentDetail(String agentCode) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        List<SimpleAgentVersionResponse> versions = agentVersionService.listByAgentId(agent.getId(), agent.getTenantId())
                .stream()
                .map(version -> {
                    SimpleAgentVersionConfigDTO config = simpleAgentSupportManager.parseConfig(version.getConfigSnapshotJson());
                    return SimpleAgentAssembler.toVersionResponse(
                            version,
                            simpleAgentSupportManager.parseCapabilities(version.getSelectedCapabilitiesJson()),
                            config
                    );
                })
                .toList();
        return SimpleAgentAssembler.toDetailResponse(agent, versions);
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentSessionResponse createSession(String agentCode, SimpleAgentSessionCreateRequest request) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        validateAgentCanCreateSession(agent, request);
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
            List<String> selectedCapabilities,
            List<String> selectedHookCodes,
            SimpleAgentPromptConfigDTO promptConfig
    ) {
        int nextVersionNo = agent.getLatestVersionNo() == null ? 1 : agent.getLatestVersionNo() + 1;
        List<String> capabilities = SimpleAgentAssembler.normalizeCapabilities(selectedCapabilities);
        List<String> hookCodes = SimpleAgentAssembler.normalizeHookCodes(selectedHookCodes);
        PromptTemplateResolvedDTO promptResolved = resolvePromptConfig(promptConfig, systemPrompt);
        SimpleAgentVersionConfigDTO config = SimpleAgentAssembler.toVersionConfig(
                agentName,
                description,
                promptResolved.getEffectiveSystemPrompt(),
                capabilities,
                hookCodes,
                toAgentPromptConfig(promptResolved, promptConfig)
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

    private PromptTemplateResolvedDTO resolvePromptConfig(SimpleAgentPromptConfigDTO promptConfig, String systemPrompt) {
        if (promptConfig != null) {
            Long promptTemplateId = simpleAgentSupportManager.parseLongId(
                    promptConfig.getPromptTemplateId(),
                    "promptTemplateId"
            );
            if (promptTemplateId != null) {
                return promptTemplateResolver.resolveTemplateById(
                        promptTemplateId,
                        promptConfig.getPromptVariables()
                );
            }
            if (PromptTemplateConstants.BINDING_TYPE_CUSTOM.equalsIgnoreCase(promptConfig.getPromptBindingType())
                    || StringUtils.hasText(promptConfig.getPromptSourceType())) {
                return promptTemplateResolver.resolveCustomTemplate(PromptTemplateBindDTO.builder()
                        .promptBindingType(promptConfig.getPromptBindingType())
                        .promptSourceType(promptConfig.getPromptSourceType())
                        .promptTemplateContent(promptConfig.getPromptTemplateContent())
                        .promptTemplatePath(promptConfig.getPromptTemplatePath())
                        .build());
            }
        }
        if (StringUtils.hasText(systemPrompt)) {
            return PromptTemplateResolvedDTO.builder()
                    .promptBindingType(PromptTemplateConstants.BINDING_TYPE_CUSTOM)
                    .promptSourceType(PromptTemplateConstants.SOURCE_TYPE_INLINE)
                    .effectiveSystemPrompt(systemPrompt.trim())
                    .build();
        }
        return PromptTemplateResolvedDTO.builder()
                .promptBindingType(PromptTemplateConstants.BINDING_TYPE_CUSTOM)
                .promptSourceType(PromptTemplateConstants.SOURCE_TYPE_INLINE)
                .effectiveSystemPrompt(null)
                .build();
    }

    private SimpleAgentPromptConfigDTO toAgentPromptConfig(
            PromptTemplateResolvedDTO promptResolved,
            SimpleAgentPromptConfigDTO promptConfig
    ) {
        return SimpleAgentPromptConfigDTO.builder()
                .promptTemplateId(promptResolved.getPromptTemplateId() == null
                        ? null
                        : String.valueOf(promptResolved.getPromptTemplateId()))
                .promptTemplateCode(promptResolved.getPromptTemplateCode())
                .promptTemplateName(promptResolved.getPromptTemplateName())
                .promptBindingType(promptResolved.getPromptBindingType())
                .promptSourceType(promptResolved.getPromptSourceType())
                .promptTemplatePath(promptResolved.getPromptTemplatePath())
                .promptTemplateContent(promptConfig == null ? null : promptConfig.getPromptTemplateContent())
                .promptVariableDefinitions(SimpleAgentAssembler.toPromptVariableDefinitions(promptResolved.getVariableDefinitions()))
                .promptVariables(promptResolved.getPromptVariables())
                .build();
    }

    private void validateCreateRequest(SimpleAgentCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getAgentName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "智能体名称不能为空");
        }
    }

    private void validateUpdateRequest(SimpleAgentUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getAgentName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "智能体名称不能为空");
        }
    }

    private String resolveAgentType(String agentType) {
        if (!StringUtils.hasText(agentType)) {
            return SimpleAgentConstants.AGENT_TYPE_REACT;
        }
        if (!SimpleAgentConstants.AGENT_TYPE_REACT.equalsIgnoreCase(agentType.trim())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "当前仅支持 REACT 类型的 Agent");
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
        throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到智能体版本");
    }

    /**
     * 校验 Agent 是否允许创建会话。
     *
     * <p>禁用态禁止所有会话入口；默认会话只能绑定已发布版本，避免草稿态误入默认链路。</p>
     */
    private void validateAgentCanCreateSession(Agent agent, SimpleAgentSessionCreateRequest request) {
        if (SimpleAgentConstants.AGENT_STATUS_DISABLED.equals(agent.getAgentStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "已禁用的 Agent 不允许创建会话");
        }

        Integer versionNo = request == null ? null : request.getVersionNo();
        if (versionNo == null && agent.getPublishedVersionId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "默认会话必须使用已发布的 Agent 版本");
        }
    }

    /**
     * 校验 Agent 是否允许删除。
     *
     * <p>仅允许删除已禁用 Agent，避免误删仍处于草稿或已发布状态的运行配置。</p>
     */
    private void validateAgentCanDelete(Agent agent) {
        if (!SimpleAgentConstants.AGENT_STATUS_DISABLED.equals(agent.getAgentStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "仅允许删除已禁用的 Agent");
        }
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
