package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.AgentSession;

public interface AgentSessionService extends IService<AgentSession> {

    AgentSession getBySessionCode(String sessionCode, Long tenantId);
}
