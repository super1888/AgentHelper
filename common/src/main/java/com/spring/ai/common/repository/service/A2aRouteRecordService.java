package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.A2aRouteRecord;
import java.util.List;

public interface A2aRouteRecordService extends IService<A2aRouteRecord> {

    List<A2aRouteRecord> listByTenantId(Long tenantId);

    A2aRouteRecord getByRouteCode(Long tenantId, String routeCode);

    A2aRouteRecord matchRoute(Long tenantId, String sourceAgentCode, String taskType);
}
