package com.spring.ai.agent.application.manager;

import com.spring.ai.agent.application.assmbler.SimpleAgentAssembler;
import com.spring.ai.agent.domain.response.SimpleAgentWsEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.agent.domain.dto.SimpleAgentVersionConfigDTO;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.exception.BusinessExceptions;
import com.spring.ai.common.repository.enitiy.Agent;
import com.spring.ai.common.repository.enitiy.AgentSession;
import com.spring.ai.common.repository.enitiy.AgentSessionEvent;
import com.spring.ai.common.repository.enitiy.AgentTask;
import com.spring.ai.common.repository.enitiy.AgentVersion;
import com.spring.ai.common.repository.service.AgentService;
import com.spring.ai.common.repository.service.AgentSessionService;
import com.spring.ai.common.repository.service.AgentTaskService;
import com.spring.ai.common.repository.service.AgentVersionService;
import com.spring.ai.common.utils.CommonTextUtils;
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
     * 校验并获取 Agent 主档。
     */
    public Agent requireAgent(String agentCode) {
        Agent agent = agentService.getByAgentCode(agentCode, currentUserContextSupport.getCurrentTenantIdWithAutoInit());
        if (agent == null) {
            throw BusinessExceptions.notFound("未找到智能体: " + agentCode);
        }
        validateOwner(agent.getOwnerUserId());
        return agent;
    }

    /**
     * 校验并获取指定版本。
     */
    public AgentVersion requireAgentVersion(Long agentId, Integer versionNo) {
        AgentVersion version = agentVersionService.getByAgentIdAndVersionNo(agentId, currentUserContextSupport.getCurrentTenantIdWithAutoInit(), versionNo);
        if (version == null) {
            throw BusinessExceptions.notFound("未找到智能体版本: " + versionNo);
        }
        return version;
    }

    /**
     * 校验并按主键获取版本。
     */
    public AgentVersion requireAgentVersionById(Long versionId) {
        AgentVersion version = agentVersionService.getById(versionId);
        if (version == null || !sameTenant(version.getTenantId(), currentUserContextSupport.getCurrentTenantIdWithAutoInit())) {
            throw BusinessExceptions.notFound("未找到智能体版本: " + versionId);
        }
        return version;
    }

    /**
     * 校验并获取会话。
     */
    public AgentSession requireSession(String sessionCode) {
        AgentSession session = agentSessionService.getBySessionCode(sessionCode, currentUserContextSupport.getCurrentTenantIdWithAutoInit());
        if (session == null) {
            throw BusinessExceptions.notFound("未找到会话: " + sessionCode);
        }
        validateOwner(session.getOwnerUserId());
        return session;
    }

    /**
     * 校验并获取任务。
     */
    public AgentTask requireTask(String taskCode) {
        AgentTask task = agentTaskService.getByTaskCode(taskCode, currentUserContextSupport.getCurrentTenantIdWithAutoInit());
        if (task == null) {
            throw BusinessExceptions.notFound("未找到任务: " + taskCode);
        }
        validateOwner(task.getOwnerUserId());
        return task;
    }

    /**
     * 校验当前用户是否拥有资源访问权。
     */
    public void validateOwner(Long ownerUserId) {
        if (ownerUserId == null || !ownerUserId.equals(currentUserContextSupport.getCurrentUserId())) {
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

    /**
     * 将字符串主键安全转换为 Long，避免前端传递超长整型时出现精度问题。
     */
    public Long parseLongId(String rawId, String fieldName) {
        if (!StringUtils.hasText(rawId)) {
            return null;
        }
        String normalizedId = rawId.trim();
        if (!normalizedId.matches("\\d+")) {
            throw new BusinessException(
                    ErrorCodeEnum.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    fieldName + " 必须为数字字符串");
        }
        try {
            return Long.valueOf(normalizedId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(
                    ErrorCodeEnum.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    fieldName + " 超出 Long 范围",
                    exception);
        }
    }

    /**
     * 统一组装事件回放载荷，避免多个管理器重复拼装 taskCode、版本号和时间戳。
     */
    public SimpleAgentWsEvent buildReplayEvent(AgentSession session, AgentSessionEvent event) {
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
                        : CommonTextUtils.toEpochMilli(event.getCreateTime())
        );
    }

    /**
     * 将任务主键解析为前端可直接识别的 taskCode，查不到任务时退化为主键字符串。
     */
    public String resolveTaskCode(Long taskId) {
        if (taskId == null) {
            return null;
        }
        AgentTask task = agentTaskService.getById(taskId);
        return task == null ? String.valueOf(taskId) : task.getTaskCode();
    }

    private boolean sameTenant(Long resourceTenantId, Long currentTenantId) {
        if (resourceTenantId == null) {
            return currentTenantId == null;
        }
        return resourceTenantId.equals(currentTenantId);
    }
}
