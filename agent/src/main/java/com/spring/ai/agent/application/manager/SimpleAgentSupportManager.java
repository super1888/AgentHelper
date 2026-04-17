package com.spring.ai.agent.application.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.agent.domain.dto.SimpleAgentVersionConfigDTO;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.enums.user.UserStatusEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.providerInterface.UserProvider;
import com.spring.ai.common.repository.enitiy.Agent;
import com.spring.ai.common.repository.enitiy.AgentSession;
import com.spring.ai.common.repository.enitiy.AgentTask;
import com.spring.ai.common.repository.enitiy.AgentVersion;
import com.spring.ai.common.repository.enitiy.SyTenant;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.AgentService;
import com.spring.ai.common.repository.service.AgentSessionService;
import com.spring.ai.common.repository.service.AgentTaskService;
import com.spring.ai.common.repository.service.AgentVersionService;
import com.spring.ai.common.repository.service.SyTenantService;
import com.spring.ai.common.repository.service.SyUserService;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Agent 领域公共支持管理器。
 *
 * <p>统一处理当前用户、当前租户、资源归属校验以及 JSON 序列化，
 * 避免横切逻辑散落在各个编排类中。</p>
 */
@Component
public class SimpleAgentSupportManager {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    @Resource
    private UserProvider userProvider;

    @Resource
    private SyUserService syUserService;

    @Resource
    private SyTenantService syTenantService;

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

    public Long getCurrentUserId() {
        Long userId = userProvider.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "未找到当前用户");
        }
        return userId;
    }

    public String getCurrentUserName() {
        return userProvider.getCurrentUserName();
    }

    public Long getCurrentTenantId() {
        SyUser user = syUserService.getDetailById(getCurrentUserId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "未找到当前用户");
        }
        return resolveTenantId(user);
    }

    public Agent requireAgent(String agentCode) {
        Agent agent = agentService.getByAgentCode(agentCode, getCurrentTenantId());
        if (agent == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到智能体：" + agentCode);
        }
        validateOwner(agent.getOwnerUserId());
        return agent;
    }

    public AgentVersion requireAgentVersion(Long agentId, Integer versionNo) {
        AgentVersion version = agentVersionService.getByAgentIdAndVersionNo(agentId, getCurrentTenantId(), versionNo);
        if (version == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND,
                    "未找到智能体版本：" + versionNo);
        }
        return version;
    }

    public AgentVersion requireAgentVersionById(Long versionId) {
        AgentVersion version = agentVersionService.getById(versionId);
        if (version == null || !sameTenant(version.getTenantId(), getCurrentTenantId())) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND,
                    "未找到智能体版本：" + versionId);
        }
        return version;
    }

    public AgentSession requireSession(String sessionCode) {
        AgentSession session = agentSessionService.getBySessionCode(sessionCode, getCurrentTenantId());
        if (session == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND,
                    "未找到会话：" + sessionCode);
        }
        validateOwner(session.getOwnerUserId());
        return session;
    }

    public AgentTask requireTask(String taskCode) {
        AgentTask task = agentTaskService.getByTaskCode(taskCode, getCurrentTenantId());
        if (task == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND,
                    "未找到任务：" + taskCode);
        }
        validateOwner(task.getOwnerUserId());
        return task;
    }

    public void validateOwner(Long ownerUserId) {
        if (ownerUserId == null || !ownerUserId.equals(getCurrentUserId())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN, HttpStatus.FORBIDDEN,
                    "当前用户无权访问该资源");
        }
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON 序列化失败", e);
        }
    }

    public SimpleAgentVersionConfigDTO parseConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return new SimpleAgentVersionConfigDTO();
        }
        try {
            return objectMapper.readValue(json, SimpleAgentVersionConfigDTO.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON 解析失败", e);
        }
    }

    public List<String> parseCapabilities(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "JSON 解析失败", e);
        }
    }

    /**
     * 解析当前用户租户。
     *
     * <p>若历史用户尚未分配真实租户，会自动补建默认租户记录，
     * 再将用户绑定到该默认租户。</p>
     *
     * @param user 用户实体
     * @return 租户 ID
     */
    private Long resolveTenantId(SyUser user) {
        if (user.getTenantId() != null) {
            SyTenant existingTenant = syTenantService.getDetailById(user.getTenantId());
            if (existingTenant != null) {
                return existingTenant.getId();
            }
        }

        SyTenant defaultTenant = syTenantService.getDefaultTenantByOwnerUserId(user.getId());
        if (defaultTenant == null) {
            defaultTenant = new SyTenant();
            defaultTenant.setTenantCode("DEFAULT_" + user.getId());
            defaultTenant.setTenantName(buildDefaultTenantName(user));
            defaultTenant.setStatus(UserStatusEnum.ENABLE.getCode());
            defaultTenant.setIsDefault(1);
            defaultTenant.setOwnerUserId(user.getId());
            defaultTenant.setOwnerUserName(user.getUsername());
            defaultTenant.setContactName(user.getNickname());
            defaultTenant.setContactPhone(user.getPhone());
            defaultTenant.setDescription("系统自动初始化的默认租户");
            syTenantService.save(defaultTenant);
        }

        user.setTenantId(defaultTenant.getId());
        syUserService.updateById(user);
        return user.getTenantId();
    }

    private String buildDefaultTenantName(SyUser user) {
        String baseName = StringUtils.hasText(user.getNickname()) ? user.getNickname().trim() : user.getUsername();
        return baseName + "默认租户";
    }

    private boolean sameTenant(Long resourceTenantId, Long currentTenantId) {
        if (resourceTenantId == null) {
            return currentTenantId == null;
        }
        return resourceTenantId.equals(currentTenantId);
    }
}
