package com.spring.ai.agent.application.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring.ai.agent.application.assmbler.SimpleAgentAssembler;
import com.spring.ai.agent.domain.dto.SimpleAgentModelBindingDTO;
import com.spring.ai.agent.domain.dto.SimpleAgentPromptConfigDTO;
import com.spring.ai.agent.domain.dto.SimpleAgentVersionConfigDTO;
import com.spring.ai.agent.domain.request.SimpleAgentCreateRequest;
import com.spring.ai.agent.domain.request.SimpleAgentBatchMigrateModelRequest;
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
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.core.application.manager.CoreApplicationManager;
import com.spring.ai.core.domain.response.ModelOptionResponse;
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

    @Resource
    private PromptTemplateResolver promptTemplateResolver;

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private CoreApplicationManager coreApplicationManager;

    public List<SimpleAgentSummaryResponse> listAgents() {
        return listAgents(null);
    }

    public List<SimpleAgentSummaryResponse> listAgents(String modelCode) {
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        Long currentUserId = currentUserContextSupport.getCurrentUserId();
        return agentService.listByOwner(tenantId, currentUserId)
                .stream()
                .map(agent -> new Object[] {agent, resolveCurrentVersionConfig(agent)})
                .filter(item -> !StringUtils.hasText(modelCode)
                        || hasModelBinding((SimpleAgentVersionConfigDTO) item[1], modelCode))
                .map(item -> SimpleAgentAssembler.toSummaryResponse(
                        (Agent) item[0],
                        (SimpleAgentVersionConfigDTO) item[1]
                ))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchMigrateModels(SimpleAgentBatchMigrateModelRequest request) {
        if (request == null || request.getAgentIds() == null || request.getAgentIds().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请选择要迁移的 Agent");
        }
        if (!StringUtils.hasText(request.getTargetModelConfigCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请选择目标模型");
        }
        String migrationMode = resolveMigrationMode(request.getMigrationMode());
        SimpleAgentModelBindingDTO targetBinding = resolveModelBinding(request.getTargetModelConfigCode());
        for (String agentCode : request.getAgentIds()) {
            Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
            SimpleAgentVersionConfigDTO currentConfig = resolveCurrentVersionConfig(agent);
            if (currentConfig == null) {
                continue;
            }
            SimpleAgentVersionConfigDTO nextConfig = SimpleAgentVersionConfigDTO.builder()
                    .agentName(currentConfig.getAgentName())
                    .description(currentConfig.getDescription())
                    .systemPrompt(currentConfig.getSystemPrompt())
                    .selectedCapabilities(currentConfig.getSelectedCapabilities())
                    .selectedHookCodes(currentConfig.getSelectedHookCodes())
                    .promptConfig(currentConfig.getPromptConfig())
                    .modelBinding(targetBinding)
                    .build();
            int nextVersionNo = agent.getLatestVersionNo() == null ? 1 : agent.getLatestVersionNo() + 1;
            AgentVersion version = SimpleAgentAssembler.toCreateVersion(
                    agent,
                    nextVersionNo,
                    nextConfig,
                    simpleAgentSupportManager.toJson(SimpleAgentAssembler.normalizeCapabilities(currentConfig.getSelectedCapabilities())),
                    simpleAgentSupportManager.toJson(nextConfig)
            );
            agentVersionService.save(version);
            agent.setCurrentVersionId(version.getId());
            agent.setLatestVersionNo(version.getVersionNo());
            if ("PUBLISH_NEW_VERSION".equals(migrationMode)) {
                publishMigratedVersion(agent, version);
            } else if (SimpleAgentConstants.AGENT_STATUS_DISABLED.equals(agent.getAgentStatus())) {
                agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_DRAFT);
            }
            agentService.updateById(agent);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentCreateResponse createAgent(SimpleAgentCreateRequest request) {
        validateCreateRequest(request);
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        Long currentUserId = currentUserContextSupport.getCurrentUserId();
        String currentUserName = currentUserContextSupport.getCurrentUserName();

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
                request.getSelectedCapabilities(),
                request.getSelectedHookCodes(),
                request.getPromptConfig(),
                request.getModelConfigCode()
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

    @Transactional(rollbackFor = Exception.class)
    public SimpleAgentCreateResponse updateAgent(String agentCode, SimpleAgentUpdateRequest request) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        validateUpdateRequest(request);

        AgentVersion version = createVersion(
                agent,
                request.getAgentName(),
                request.getDescription(),
                request.getSystemPrompt(),
                request.getSelectedCapabilities(),
                request.getSelectedHookCodes(),
                request.getPromptConfig(),
                request.getModelConfigCode()
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

    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(String agentCode) {
        Agent agent = simpleAgentSupportManager.requireAgent(agentCode);
        validateAgentCanDelete(agent);

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
                .map(event -> simpleAgentSupportManager.buildReplayEvent(session, event))
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
            SimpleAgentPromptConfigDTO promptConfig,
            String modelConfigCode
    ) {
        int nextVersionNo = agent.getLatestVersionNo() == null ? 1 : agent.getLatestVersionNo() + 1;
        List<String> capabilities = SimpleAgentAssembler.normalizeCapabilities(selectedCapabilities);
        List<String> hookCodes = SimpleAgentAssembler.normalizeHookCodes(selectedHookCodes);
        PromptTemplateResolvedDTO promptResolved = resolvePromptConfig(promptConfig, systemPrompt);
        SimpleAgentModelBindingDTO modelBinding = resolveModelBinding(modelConfigCode);
        SimpleAgentVersionConfigDTO config = SimpleAgentAssembler.toVersionConfig(
                agentName,
                description,
                promptResolved.getEffectiveSystemPrompt(),
                capabilities,
                hookCodes,
                toAgentPromptConfig(promptResolved, promptConfig),
                modelBinding
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
        /**
         * 将模板解析后的快照完整写入 Agent 版本配置，确保运行时可直接消费模板变量和企业级策略。
         */
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
                .enterpriseConfig(promptResolved.getEnterpriseConfig())
                .build();
    }

    private SimpleAgentModelBindingDTO resolveModelBinding(String modelConfigCode) {
        ModelOptionResponse option = coreApplicationManager.getEnabledModelOption(modelConfigCode);
        return SimpleAgentModelBindingDTO.builder()
                .modelCode(option.getModelCode())
                .modelName(option.getModelName())
                .providerConfigCode(option.getProviderConfigCode())
                .providerEnum(option.getProviderEnum())
                .providerName(option.getProviderName())
                .modelIdentifier(option.getModelIdentifier())
                .modelType(option.getModelType())
                .build();
    }

    private SimpleAgentVersionConfigDTO resolveCurrentVersionConfig(Agent agent) {
        if (agent == null || agent.getCurrentVersionId() == null) {
            return null;
        }
        AgentVersion version = agentVersionService.getById(agent.getCurrentVersionId());
        if (version == null || !StringUtils.hasText(version.getConfigSnapshotJson())) {
            return null;
        }
        return simpleAgentSupportManager.parseConfig(version.getConfigSnapshotJson());
    }

    private boolean hasModelBinding(SimpleAgentVersionConfigDTO config, String modelCode) {
        return config != null
                && config.getModelBinding() != null
                && modelCode.trim().equals(config.getModelBinding().getModelCode());
    }

    /**
     * 解析批量迁移模式，默认只生成草稿版本。
     */
    private String resolveMigrationMode(String migrationMode) {
        if (!StringUtils.hasText(migrationMode)) {
            return "DRAFT_ONLY";
        }
        String normalized = migrationMode.trim().toUpperCase();
        if (!"DRAFT_ONLY".equals(normalized) && !"PUBLISH_NEW_VERSION".equals(normalized)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "不支持的模型迁移模式");
        }
        return normalized;
    }

    /**
     * 发布迁移后的新版本，并同步 Agent 发布信息。
     */
    private void publishMigratedVersion(Agent agent, AgentVersion version) {
        agentVersionService.update(Wrappers.lambdaUpdate(AgentVersion.class)
                .eq(AgentVersion::getAgentId, agent.getId())
                .eq(AgentVersion::getTenantId, agent.getTenantId())
                .set(AgentVersion::getIsPublished, 0));
        version.setIsPublished(1);
        agentVersionService.updateById(version);
        agent.setPublishedVersionId(version.getId());
        agent.setPublishedVersionNo(version.getVersionNo());
        agent.setAgentStatus(SimpleAgentConstants.AGENT_STATUS_PUBLISHED);
    }

    private void validateCreateRequest(SimpleAgentCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getAgentName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "鏅鸿兘浣撳悕绉颁笉鑳戒负绌�");
        }
        if (request == null || !StringUtils.hasText(request.getModelConfigCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "智能体必须绑定模型配置");
        }
    }

    private void validateUpdateRequest(SimpleAgentUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getAgentName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "鏅鸿兘浣撳悕绉颁笉鑳戒负绌�");
        }
        if (request == null || !StringUtils.hasText(request.getModelConfigCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "智能体必须绑定模型配置");
        }
    }

    private String resolveAgentType(String agentType) {
        if (!StringUtils.hasText(agentType)) {
            return SimpleAgentConstants.AGENT_TYPE_REACT;
        }
        if (!SimpleAgentConstants.AGENT_TYPE_REACT.equalsIgnoreCase(agentType.trim())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "褰撳墠浠呮敮鎸� REACT 绫诲瀷鐨� Agent");
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
        throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "鏈壘鍒版櫤鑳戒綋鐗堟湰");
    }

    private void validateAgentCanCreateSession(Agent agent, SimpleAgentSessionCreateRequest request) {
        if (SimpleAgentConstants.AGENT_STATUS_DISABLED.equals(agent.getAgentStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "宸茬鐢ㄧ殑 Agent 涓嶅厑璁稿垱寤轰細璇�");
        }

        Integer versionNo = request == null ? null : request.getVersionNo();
        if (versionNo == null && agent.getPublishedVersionId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "榛樿浼氳瘽蹇呴』浣跨敤宸插彂甯冪殑 Agent 鐗堟湰");
        }
    }

    private void validateAgentCanDelete(Agent agent) {
        if (!SimpleAgentConstants.AGENT_STATUS_DISABLED.equals(agent.getAgentStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "浠呭厑璁稿垹闄ゅ凡绂佺敤鐨� Agent");
        }
    }
}
