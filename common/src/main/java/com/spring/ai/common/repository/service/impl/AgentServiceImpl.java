package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.AgentMapper;
import com.spring.ai.common.repository.enitiy.Agent;
import com.spring.ai.common.repository.service.AgentService;
import com.spring.ai.common.constants.SqlConstants;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    @Override
    public Agent getByAgentCode(String agentCode, Long tenantId) {
        return getOne(Wrappers.lambdaQuery(Agent.class)
                .eq(Agent::getAgentCode, agentCode)
                .eq(Agent::getTenantId, tenantId)
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public List<Agent> listByOwner(Long tenantId, Long ownerUserId) {
        return list(Wrappers.lambdaQuery(Agent.class)
                .eq(Agent::getTenantId, tenantId)
                .eq(Agent::getOwnerUserId, ownerUserId)
                .orderByDesc(Agent::getUpdateTime)
                .orderByDesc(Agent::getId));
    }

    @Override
    public long countByTenantId(Long tenantId) {
        if (tenantId == null) {
            return 0L;
        }
        return count(Wrappers.lambdaQuery(Agent.class).eq(Agent::getTenantId, tenantId));
    }
}
