package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.SyTenantMapper;
import com.spring.ai.common.repository.enitiy.SyTenant;
import com.spring.ai.common.repository.service.SyTenantService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 租户仓储服务实现。
 */
@Service
public class SyTenantServiceImpl extends ServiceImpl<SyTenantMapper, SyTenant> implements SyTenantService {

    @Override
    public SyTenant getByTenantCode(String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(SyTenant.class)
                .eq(SyTenant::getTenantCode, tenantCode.trim())
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public SyTenant getDetailById(Long tenantId) {
        return getById(tenantId);
    }

    @Override
    public SyTenant getDefaultTenantByOwnerUserId(Long ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(SyTenant.class)
                .eq(SyTenant::getOwnerUserId, ownerUserId)
                .eq(SyTenant::getIsDefault, 1)
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public List<SyTenant> listAllTenants() {
        return list(Wrappers.lambdaQuery(SyTenant.class)
                .orderByDesc(SyTenant::getUpdateTime)
                .orderByDesc(SyTenant::getId));
    }

    @Override
    public List<SyTenant> pageQueryTenants(String tenantName, String tenantCode, Integer status) {
        return list(buildLambdaQuery(tenantName, tenantCode, status));
    }

    @Override
    public long countAllTenants() {
        return count();
    }

    @Override
    public long countByStatus(Integer status) {
        if (status == null) {
            return count();
        }
        return count(Wrappers.lambdaQuery(SyTenant.class).eq(SyTenant::getStatus, status));
    }

    private LambdaQueryWrapper<SyTenant> buildLambdaQuery(String tenantName, String tenantCode, Integer status) {
        LambdaQueryWrapper<SyTenant> queryWrapper = Wrappers.lambdaQuery(SyTenant.class)
                .orderByDesc(SyTenant::getUpdateTime)
                .orderByDesc(SyTenant::getId);

        if (StringUtils.hasText(tenantName)) {
            queryWrapper.like(SyTenant::getTenantName, tenantName.trim());
        }
        if (StringUtils.hasText(tenantCode)) {
            queryWrapper.like(SyTenant::getTenantCode, tenantCode.trim());
        }
        if (status != null) {
            queryWrapper.eq(SyTenant::getStatus, status);
        }
        return queryWrapper;
    }
}
