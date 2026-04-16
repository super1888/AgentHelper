package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.AgentSessionMapper;
import com.spring.ai.common.repository.enitiy.AgentSession;
import com.spring.ai.common.repository.service.AgentSessionService;
import com.spring.ai.common.constants.SqlConstants;
import org.springframework.stereotype.Service;

@Service
public class AgentSessionServiceImpl extends ServiceImpl<AgentSessionMapper, AgentSession>
        implements AgentSessionService {

    @Override
    public AgentSession getBySessionCode(String sessionCode, Long tenantId) {
        return getOne(Wrappers.lambdaQuery(AgentSession.class)
                .eq(AgentSession::getSessionCode, sessionCode)
                .eq(AgentSession::getTenantId, tenantId)
                .last(SqlConstants.LIMIT_ONE));
    }
}
