package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.AgentVersion;
import java.util.List;

public interface AgentVersionService extends IService<AgentVersion> {

    List<AgentVersion> listByAgentId(Long agentId, Long tenantId);

    AgentVersion getByAgentIdAndVersionNo(Long agentId, Long tenantId, Integer versionNo);
}
