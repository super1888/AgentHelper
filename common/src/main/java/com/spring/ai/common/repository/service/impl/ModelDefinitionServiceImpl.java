package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.ModelDefinitionMapper;
import com.spring.ai.common.repository.enitiy.ModelDefinition;
import com.spring.ai.common.repository.service.ModelDefinitionService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ModelDefinitionServiceImpl extends ServiceImpl<ModelDefinitionMapper, ModelDefinition>
        implements ModelDefinitionService {

    @Override
    public List<ModelDefinition> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(ModelDefinition.class)
                .eq(ModelDefinition::getTenantId, tenantId)
                .orderByDesc(ModelDefinition::getIsDefault)
                .orderByDesc(ModelDefinition::getUpdateTime)
                .orderByDesc(ModelDefinition::getId));
    }

    @Override
    public List<ModelDefinition> listEnabledByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(ModelDefinition.class)
                .eq(ModelDefinition::getTenantId, tenantId)
                .eq(ModelDefinition::getStatus, "ENABLED")
                .orderByDesc(ModelDefinition::getIsDefault)
                .orderByDesc(ModelDefinition::getUpdateTime)
                .orderByDesc(ModelDefinition::getId));
    }

    @Override
    public ModelDefinition getByModelCode(Long tenantId, String modelCode) {
        if (tenantId == null || !StringUtils.hasText(modelCode)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(ModelDefinition.class)
                .eq(ModelDefinition::getTenantId, tenantId)
                .eq(ModelDefinition::getModelCode, modelCode.trim())
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public List<ModelDefinition> listByProviderConfigId(Long tenantId, Long providerConfigId) {
        return list(Wrappers.lambdaQuery(ModelDefinition.class)
                .eq(ModelDefinition::getTenantId, tenantId)
                .eq(ModelDefinition::getProviderConfigId, providerConfigId)
                .orderByDesc(ModelDefinition::getIsDefault)
                .orderByDesc(ModelDefinition::getUpdateTime)
                .orderByDesc(ModelDefinition::getId));
    }
}
