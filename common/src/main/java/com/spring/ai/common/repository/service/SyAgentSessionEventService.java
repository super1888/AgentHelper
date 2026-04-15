package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.SyAgentSessionEvent;
import java.util.List;

public interface SyAgentSessionEventService extends IService<SyAgentSessionEvent> {

    List<SyAgentSessionEvent> listReplayEvents(Long sessionId, Long tenantId, Long afterSequence);
}
