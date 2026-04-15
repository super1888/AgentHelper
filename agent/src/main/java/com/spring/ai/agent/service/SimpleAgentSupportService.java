package com.spring.ai.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.agent.domain.dto.SimpleAgentVersionConfigDTO;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.providerInterface.UserProvider;
import com.spring.ai.common.repository.enitiy.SyAgent;
import com.spring.ai.common.repository.enitiy.SyAgentSession;
import com.spring.ai.common.repository.enitiy.SyAgentTask;
import com.spring.ai.common.repository.enitiy.SyAgentVersion;
import com.spring.ai.common.repository.service.SyAgentService;
import com.spring.ai.common.repository.service.SyAgentSessionService;
import com.spring.ai.common.repository.service.SyAgentTaskService;
import com.spring.ai.common.repository.service.SyAgentVersionService;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyUserService;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Agent 领域公共支撑服务。
 *
 * <p>负责当前用户/租户解析、资源归属校验以及 JSON 序列化，
 * 避免这些横切逻辑散落在业务代码中。</p>
 */
@Service
public class SimpleAgentSupportService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    @Resource
    private UserProvider userProvider;

    @Resource
    private SyUserService syUserService;

    @Resource
    private SyAgentService syAgentService;

    @Resource
    private SyAgentVersionService syAgentVersionService;

    @Resource
    private SyAgentSessionService syAgentSessionService;

    @Resource
    private SyAgentTaskService syAgentTaskService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 获取当前登录用户 ID。
     */
    public Long getCurrentUserId() {
        Long userId = userProvider.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "current user not found");
        }
        return userId;
    }

    /**
     * 获取当前登录用户名。
     */
    public String getCurrentUserName() {
        return userProvider.getCurrentUserName();
    }

    /**
     * 获取当前用户所属租户。
     */
    public Long getCurrentTenantId() {
        SyUser user = syUserService.getDetailById(getCurrentUserId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "current user not found");
        }
        return user.getTenantId();
    }

    /**
     * 按编码获取 Agent，并校验归属权限。
     */
    public SyAgent requireAgent(String agentCode) {
        SyAgent agent = syAgentService.getByAgentCode(agentCode, getCurrentTenantId());
        if (agent == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "agent not found: " + agentCode);
        }
        validateOwner(agent.getOwnerUserId());
        return agent;
    }

    /**
     * 按版本号获取 Agent 版本。
     */
    public SyAgentVersion requireAgentVersion(Long agentId, Integer versionNo) {
        SyAgentVersion version = syAgentVersionService.getByAgentIdAndVersionNo(agentId, getCurrentTenantId(), versionNo);
        if (version == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND,
                    "agent version not found: " + versionNo);
        }
        return version;
    }

    /**
     * 按主键获取 Agent 版本。
     */
    public SyAgentVersion requireAgentVersionById(Long versionId) {
        SyAgentVersion version = syAgentVersionService.getById(versionId);
        if (version == null || !sameTenant(version.getTenantId(), getCurrentTenantId())) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND,
                    "agent version not found: " + versionId);
        }
        return version;
    }

    /**
     * 按编码获取会话，并校验归属权限。
     */
    public SyAgentSession requireSession(String sessionCode) {
        SyAgentSession session = syAgentSessionService.getBySessionCode(sessionCode, getCurrentTenantId());
        if (session == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND,
                    "session not found: " + sessionCode);
        }
        validateOwner(session.getOwnerUserId());
        return session;
    }

    /**
     * 按编码获取任务，并校验归属权限。
     */
    public SyAgentTask requireTask(String taskCode) {
        SyAgentTask task = syAgentTaskService.getByTaskCode(taskCode, getCurrentTenantId());
        if (task == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND,
                    "task not found: " + taskCode);
        }
        validateOwner(task.getOwnerUserId());
        return task;
    }

    /**
     * 校验资源是否属于当前用户。
     */
    public void validateOwner(Long ownerUserId) {
        if (ownerUserId == null || !ownerUserId.equals(getCurrentUserId())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN, HttpStatus.FORBIDDEN,
                    "current user has no permission to access this resource");
        }
    }

    /**
     * 对象转 JSON。
     */
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "json serialize failed", e);
        }
    }

    /**
     * 解析版本快照配置。
     */
    public SimpleAgentVersionConfigDTO parseConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return new SimpleAgentVersionConfigDTO();
        }
        try {
            return objectMapper.readValue(json, SimpleAgentVersionConfigDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "json parse failed", e);
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
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "json parse failed", e);
        }
    }

    private boolean sameTenant(Long resourceTenantId, Long currentTenantId) {
        if (resourceTenantId == null) {
            return currentTenantId == null;
        }
        return resourceTenantId.equals(currentTenantId);
    }
}
