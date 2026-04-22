package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.A2aAgentCardRecord;
import java.util.List;

public interface A2aAgentCardRecordService extends IService<A2aAgentCardRecord> {

    List<A2aAgentCardRecord> listByTenantId(Long tenantId);

    List<A2aAgentCardRecord> listDeletedByTenantId(Long tenantId);

    A2aAgentCardRecord getByAgentCode(Long tenantId, String agentCode);
}
