package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SyAgent;
import java.util.List;

public interface SyAgentService extends IService<SyAgent> {

    SyAgent getByAgentCode(String agentCode, Long tenantId);

    List<SyAgent> listByOwner(Long tenantId, Long ownerUserId);
}
