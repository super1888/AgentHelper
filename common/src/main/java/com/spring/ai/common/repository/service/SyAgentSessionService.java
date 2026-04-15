package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SyAgentSession;

public interface SyAgentSessionService extends IService<SyAgentSession> {

    SyAgentSession getBySessionCode(String sessionCode, Long tenantId);
}
