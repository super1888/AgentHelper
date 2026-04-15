package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SyAgentVersion;
import java.util.List;

public interface SyAgentVersionService extends IService<SyAgentVersion> {

    List<SyAgentVersion> listByAgentId(Long agentId, Long tenantId);

    SyAgentVersion getByAgentIdAndVersionNo(Long agentId, Long tenantId, Integer versionNo);
}
