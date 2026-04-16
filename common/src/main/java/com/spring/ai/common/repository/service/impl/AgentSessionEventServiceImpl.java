package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.AgentSessionEventMapper;
import com.spring.ai.common.repository.enitiy.AgentSessionEvent;
import com.spring.ai.common.repository.service.AgentSessionEventService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentSessionEventServiceImpl extends ServiceImpl<AgentSessionEventMapper, AgentSessionEvent>
        implements AgentSessionEventService {

    @Override
    public List<AgentSessionEvent> listReplayEvents(Long sessionId, Long tenantId, Long afterSequence) {
        return list(Wrappers.lambdaQuery(AgentSessionEvent.class)
                .eq(AgentSessionEvent::getSessionId, sessionId)
                .eq(AgentSessionEvent::getTenantId, tenantId)
                .ge(afterSequence != null, AgentSessionEvent::getEventSequence, afterSequence + 1)
                .eq(AgentSessionEvent::getReplayable, 1)
                .orderByAsc(AgentSessionEvent::getEventSequence));
    }
}
