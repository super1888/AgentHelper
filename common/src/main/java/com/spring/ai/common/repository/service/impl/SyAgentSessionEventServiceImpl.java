package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.SyAgentSessionEventMapper;
import com.spring.ai.common.repository.enitiy.SyAgentSessionEvent;
import com.spring.ai.common.repository.service.SyAgentSessionEventService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SyAgentSessionEventServiceImpl extends ServiceImpl<SyAgentSessionEventMapper, SyAgentSessionEvent>
        implements SyAgentSessionEventService {

    @Override
    public List<SyAgentSessionEvent> listReplayEvents(Long sessionId, Long tenantId, Long afterSequence) {
        return list(Wrappers.lambdaQuery(SyAgentSessionEvent.class)
                .eq(SyAgentSessionEvent::getSessionId, sessionId)
                .eq(SyAgentSessionEvent::getTenantId, tenantId)
                .ge(afterSequence != null, SyAgentSessionEvent::getEventSequence, afterSequence + 1)
                .eq(SyAgentSessionEvent::getReplayable, 1)
                .orderByAsc(SyAgentSessionEvent::getEventSequence));
    }
}
