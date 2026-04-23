package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.ModelDefinition;
import java.util.List;

public interface ModelDefinitionService extends IService<ModelDefinition> {

    List<ModelDefinition> listByTenantId(Long tenantId);

    List<ModelDefinition> listEnabledByTenantId(Long tenantId);

    ModelDefinition getByModelCode(Long tenantId, String modelCode);

    List<ModelDefinition> listByProviderConfigId(Long tenantId, Long providerConfigId);
}
