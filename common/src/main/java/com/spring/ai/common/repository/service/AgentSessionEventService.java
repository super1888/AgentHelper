package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.AgentSessionEvent;
import java.util.List;

public interface AgentSessionEventService extends IService<AgentSessionEvent> {

    List<AgentSessionEvent> listReplayEvents(Long sessionId, Long tenantId, Long afterSequence);
}
