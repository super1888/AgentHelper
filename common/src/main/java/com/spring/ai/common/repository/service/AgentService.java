package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.Agent;
import java.util.List;

public interface AgentService extends IService<Agent> {

    Agent getByAgentCode(String agentCode, Long tenantId);

    List<Agent> listByOwner(Long tenantId, Long ownerUserId);

    long countByTenantId(Long tenantId);
}
