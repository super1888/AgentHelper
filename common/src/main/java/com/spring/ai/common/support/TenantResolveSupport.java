package com.spring.ai.common.support;

import com.spring.ai.common.repository.enitiy.SyTenant;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyTenantService;
import com.spring.ai.common.repository.service.SyUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 租户解析公共支持类。
 */
@Component
public class TenantResolveSupport {

    @Resource
    private SyTenantService syTenantService;

    @Resource
    private SyUserService syUserService;

    /**
     * 根据租户 ID 解析租户名称。
     */
    public String resolveTenantName(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        SyTenant tenant = syTenantService.getDetailById(tenantId);
        return tenant == null ? null : tenant.getTenantName();
    }

    /**
     * 根据用户 ID 解析所属租户 ID。
     */
    public Long resolveTenantIdByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        SyUser user = syUserService.getDetailById(userId);
        return user == null ? null : user.getTenantId();
    }
}
