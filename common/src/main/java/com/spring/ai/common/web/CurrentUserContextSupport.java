package com.spring.ai.common.web;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.enums.user.UserStatusEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.providerInterface.UserProvider;
import com.spring.ai.common.repository.enitiy.SyTenant;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyTenantService;
import com.spring.ai.common.repository.service.SyUserService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：统一封装当前登录用户与租户上下文解析
 * 作者：Codex
 * 创建时间：2026-04-17
 * 核心功能：提供当前用户 ID、用户名、租户与自动初始化默认租户等公共能力
 */
@Component
public class CurrentUserContextSupport {

    @Resource
    private UserProvider userProvider;

    @Resource
    private SyUserService syUserService;

    @Resource
    private SyTenantService syTenantService;

    /**
     * 获取当前登录用户 ID。
     */
    public Long getCurrentUserId() {
        Long userId = userProvider.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "未找到当前用户");
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
     * 获取当前登录用户实体。
     */
    public SyUser getCurrentUser() {
        SyUser user = syUserService.getDetailById(getCurrentUserId());
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "未找到当前用户");
        }
        return user;
    }

    /**
     * 获取当前租户 ID；若租户不存在则直接报错。
     */
    public Long getCurrentTenantId() {
        return getCurrentTenant().getId();
    }

    /**
     * 获取当前租户实体；若租户不存在则直接报错。
     */
    public SyTenant getCurrentTenant() {
        return resolveCurrentTenant(false);
    }

    /**
     * 获取当前租户 ID；当用户尚未绑定租户时自动补建默认租户。
     */
    public Long getCurrentTenantIdWithAutoInit() {
        return getCurrentTenantWithAutoInit().getId();
    }

    /**
     * 获取当前租户实体；当用户尚未绑定租户时自动补建默认租户。
     */
    public SyTenant getCurrentTenantWithAutoInit() {
        return resolveCurrentTenant(true);
    }

    private SyTenant resolveCurrentTenant(boolean autoInitDefaultTenant) {
        SyUser user = getCurrentUser();
        if (user.getTenantId() != null) {
            SyTenant tenant = syTenantService.getDetailById(user.getTenantId());
            if (tenant != null) {
                return tenant;
            }
        }

        if (!autoInitDefaultTenant) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "未找到当前租户");
        }

        SyTenant tenant = syTenantService.getDefaultTenantByOwnerUserId(user.getId());
        if (tenant == null) {
            tenant = buildDefaultTenant(user);
            syTenantService.save(tenant);
        }

        user.setTenantId(tenant.getId());
        syUserService.updateById(user);
        return tenant;
    }

    private SyTenant buildDefaultTenant(SyUser user) {
        SyTenant tenant = new SyTenant();
        tenant.setTenantCode("DEFAULT_" + user.getId());
        tenant.setTenantName(buildDefaultTenantName(user));
        tenant.setStatus(UserStatusEnum.ENABLE.getCode());
        tenant.setIsDefault(1);
        tenant.setOwnerUserId(user.getId());
        tenant.setOwnerUserName(user.getUsername());
        tenant.setContactName(user.getNickname());
        tenant.setContactPhone(user.getPhone());
        tenant.setDescription("系统自动初始化的默认租户");
        return tenant;
    }

    private String buildDefaultTenantName(SyUser user) {
        String baseName = StringUtils.hasText(user.getNickname()) ? user.getNickname().trim() : user.getUsername();
        return baseName + "默认租户";
    }
}
