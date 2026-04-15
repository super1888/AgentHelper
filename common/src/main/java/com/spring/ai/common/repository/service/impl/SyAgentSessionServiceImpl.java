package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.SyAgentSessionMapper;
import com.spring.ai.common.repository.enitiy.SyAgentSession;
import com.spring.ai.common.repository.service.SyAgentSessionService;
import com.spring.ai.common.constants.SqlConstants;
import org.springframework.stereotype.Service;

@Service
public class SyAgentSessionServiceImpl extends ServiceImpl<SyAgentSessionMapper, SyAgentSession>
        implements SyAgentSessionService {

    @Override
    public SyAgentSession getBySessionCode(String sessionCode, Long tenantId) {
        return getOne(Wrappers.lambdaQuery(SyAgentSession.class)
                .eq(SyAgentSession::getSessionCode, sessionCode)
                .eq(SyAgentSession::getTenantId, tenantId)
                .last(SqlConstants.LIMIT_ONE));
    }
}
