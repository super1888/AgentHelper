package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.ModelProviderConfig;
import java.util.List;

public interface ModelProviderConfigService extends IService<ModelProviderConfig> {

    List<ModelProviderConfig> listByTenantId(Long tenantId);

    List<ModelProviderConfig> listEnabledByTenantId(Long tenantId);

    ModelProviderConfig getByProviderConfigCode(Long tenantId, String providerConfigCode);
}
