package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.ModelProviderConfigMapper;
import com.spring.ai.common.repository.enitiy.ModelProviderConfig;
import com.spring.ai.common.repository.service.ModelProviderConfigService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ModelProviderConfigServiceImpl extends ServiceImpl<ModelProviderConfigMapper, ModelProviderConfig>
        implements ModelProviderConfigService {

    @Override
    public List<ModelProviderConfig> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(ModelProviderConfig.class)
                .eq(ModelProviderConfig::getTenantId, tenantId)
                .orderByDesc(ModelProviderConfig::getUpdateTime)
                .orderByDesc(ModelProviderConfig::getId));
    }

    @Override
    public List<ModelProviderConfig> listEnabledByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(ModelProviderConfig.class)
                .eq(ModelProviderConfig::getTenantId, tenantId)
                .eq(ModelProviderConfig::getStatus, "ENABLED")
                .orderByDesc(ModelProviderConfig::getUpdateTime)
                .orderByDesc(ModelProviderConfig::getId));
    }

    @Override
    public ModelProviderConfig getByProviderConfigCode(Long tenantId, String providerConfigCode) {
        if (tenantId == null || !StringUtils.hasText(providerConfigCode)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(ModelProviderConfig.class)
                .eq(ModelProviderConfig::getTenantId, tenantId)
                .eq(ModelProviderConfig::getProviderConfigCode, providerConfigCode.trim())
                .last(SqlConstants.LIMIT_ONE));
    }
}
