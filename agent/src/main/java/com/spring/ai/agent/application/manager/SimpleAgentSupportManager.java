package com.spring.ai.agent.application.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.agent.domain.dto.SimpleAgentVersionConfigDTO;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.Agent;
import com.spring.ai.common.repository.enitiy.AgentSession;
import com.spring.ai.common.repository.enitiy.AgentTask;
import com.spring.ai.common.repository.enitiy.AgentVersion;
import com.spring.ai.common.repository.service.AgentService;
import com.spring.ai.common.repository.service.AgentSessionService;
import com.spring.ai.common.repository.service.AgentTaskService;
import com.spring.ai.common.repository.service.AgentVersionService;
import com.spring.ai.common.web.CurrentUserContextSupport;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：Agent 领域公共支撑管理器
 * 作者：Codex
 * 创建时间：2026-04-17
 * 核心功能：委托统一用户租户上下文，并完成 Agent 资源归属校验与 JSON 解析
 */
@Component
public class SimpleAgentSupportManager {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private AgentService agentService;

    @Resource
    private AgentVersionService agentVersionService;

    @Resource
    private AgentSessionService agentSessionService;

    @Resource
    private AgentTaskService agentTaskService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 获取当前登录用户 ID。
     */
    public Long getCurrentUserId() {
        return currentUserContextSupport.getCurrentUserId();
    }

    /**
     * 获取当前登录用户名。
     */
    public String getCurrentUserName() {
        return currentUserContextSupport.getCurrentUserName();
    }

    /**
     * 获取当前租户 ID；若不存在则自动补建默认租户。
     */
    public Long getCurrentTenantId() {
        return currentUserContextSupport.getCurrentTenantIdWithAutoInit();
    }

    /**
     * 校验并获取 Agent 主档。
     */
    public Agent requireAgent(String agentCode) {
        Agent agent = agentService.getByAgentCode(agentCode, getCurrentTenantId());
        if (agent == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到智能体: " + agentCode);
        }
        validateOwner(agent.getOwnerUserId());
        return agent;
    }

    /**
     * 校验并获取指定版本。
     */
    public AgentVersion requireAgentVersion(Long agentId, Integer versionNo) {
        AgentVersion version = agentVersionService.getByAgentIdAndVersionNo(agentId, getCurrentTenantId(), versionNo);
        if (version == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到智能体版本: " + versionNo);
        }
        return version;
    }

    /**
     * 校验并按主键获取版本。
     */
    public AgentVersion requireAgentVersionById(Long versionId) {
        AgentVersion version = agentVersionService.getById(versionId);
        if (version == null || !sameTenant(version.getTenantId(), getCurrentTenantId())) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到智能体版本: " + versionId);
        }
        return version;
    }

    /**
     * 校验并获取会话。
     */
    public AgentSession requireSession(String sessionCode) {
        AgentSession session = agentSessionService.getBySessionCode(sessionCode, getCurrentTenantId());
        if (session == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到会话: " + sessionCode);
        }
        validateOwner(session.getOwnerUserId());
        return session;
    }

    /**
     * 校验并获取任务。
     */
    public AgentTask requireTask(String taskCode) {
        AgentTask task = agentTaskService.getByTaskCode(taskCode, getCurrentTenantId());
        if (task == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到任务: " + taskCode);
        }
        validateOwner(task.getOwnerUserId());
        return task;
    }

    /**
     * 校验当前用户是否拥有资源访问权。
     */
    public void validateOwner(Long ownerUserId) {
        if (ownerUserId == null || !ownerUserId.equals(getCurrentUserId())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN, HttpStatus.FORBIDDEN, "当前用户无权访问该资源");
        }
    }

    /**
     * 对对象执行 JSON 序列化。
     */
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON 序列化失败",
                    e);
        }
    }

    /**
     * 解析版本配置 JSON。
     */
    public SimpleAgentVersionConfigDTO parseConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return new SimpleAgentVersionConfigDTO();
        }
        try {
            return objectMapper.readValue(json, SimpleAgentVersionConfigDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON 解析失败",
                    e);
        }
    }

    /**
     * 解析能力列表 JSON。
     */
    public List<String> parseCapabilities(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON 解析失败",
                    e);
        }
    }

    private boolean sameTenant(Long resourceTenantId, Long currentTenantId) {
        if (resourceTenantId == null) {
            return currentTenantId == null;
        }
        return resourceTenantId.equals(currentTenantId);
    }
}
