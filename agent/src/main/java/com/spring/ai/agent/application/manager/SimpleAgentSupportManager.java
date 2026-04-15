package com.spring.ai.agent.application.manager;

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
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyAgentService;
import com.spring.ai.common.repository.service.SyAgentSessionService;
import com.spring.ai.common.repository.service.SyAgentTaskService;
import com.spring.ai.common.repository.service.SyAgentVersionService;
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
     *
     * @return 当前用户 ID
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
     *
     * @return 当前用户名
     */
    public String getCurrentUserName() {
        return userProvider.getCurrentUserName();
    }

    /**
     * 获取当前用户所属租户 ID。
     *
     * <p>这里必须返回租户而不是用户：
     * 租户负责“数据归属边界”，用户负责“登录身份与操作人”。
     * 当前项目虽然还没有独立租户中心，但 Agent、Session、Task 等表已经按租户建模，
     * 因此这里仍然保留租户解析逻辑。
     *
     * <p>如果用户尚未被分配 tenantId，则按当前阶段的默认策略自动初始化：
     * 使用用户主键生成一个默认租户编号，先保证整条租户隔离链路闭环。
     * 这只是默认租户初始化策略，不代表租户和用户是同一概念。</p>
     *
     * @return 当前租户 ID
     */
    public Long getCurrentTenantId() {
        SyUser user = syUserService.getDetailById(getCurrentUserId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "current user not found");
        }
        return resolveTenantId(user);
    }

    /**
     * 按编码获取 Agent，并校验归属权限。
     *
     * @param agentCode Agent 编码
     * @return Agent 实体
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
     *
     * @param agentId Agent 主档 ID
     * @param versionNo 版本号
     * @return 版本实体
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
     *
     * @param versionId 版本主键
     * @return 版本实体
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
     *
     * @param sessionCode 会话编码
     * @return 会话实体
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
     *
     * @param taskCode 任务编码
     * @return 任务实体
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
     * 校验当前用户是否具备资源访问权限。
     *
     * @param ownerUserId 资源拥有者 ID
     */
    public void validateOwner(Long ownerUserId) {
        if (ownerUserId == null || !ownerUserId.equals(getCurrentUserId())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN, HttpStatus.FORBIDDEN,
                    "current user has no permission to access this resource");
        }
    }

    /**
     * 对象转 JSON。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
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
     *
     * @param json 配置 JSON
     * @return 配置对象
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
     * 解析能力项列表 JSON。
     *
     * @param json 能力项 JSON
     * @return 能力项列表
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

    /**
     * 解析用户的租户编号。
     *
     * <p>用户 ID 可以作为默认租户编号的生成来源，但不能替代租户这个字段本身。
     * 原因是：
     * 1. 一个租户后续可以有多个用户；
     * 2. 资源共享、租户管理员、跨用户可见范围都依赖租户维度；
     * 3. ownerUserId 解决“谁创建/谁操作”，tenantId 解决“数据属于哪个组织边界”。
     *
     * <p>当前没有租户主表时，先将“用户主键”作为默认租户编号写入用户记录，
     * 让数据库层面继续保留 tenantId 这个独立语义。</p>
     *
     * @param user 用户实体
     * @return 租户 ID
     */
    private Long resolveTenantId(SyUser user) {
        if (user.getTenantId() != null) {
            return user.getTenantId();
        }
        user.setTenantId(user.getId());
        syUserService.updateById(user);
        return user.getTenantId();
    }

    private boolean sameTenant(Long resourceTenantId, Long currentTenantId) {
        if (resourceTenantId == null) {
            return currentTenantId == null;
        }
        return resourceTenantId.equals(currentTenantId);
    }
}
