package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.SyAgentMapper;
import com.spring.ai.common.repository.enitiy.SyAgent;
import com.spring.ai.common.repository.service.SyAgentService;
import com.spring.ai.common.constants.SqlConstants;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SyAgentServiceImpl extends ServiceImpl<SyAgentMapper, SyAgent> implements SyAgentService {

    @Override
    public SyAgent getByAgentCode(String agentCode, Long tenantId) {
        return getOne(Wrappers.lambdaQuery(SyAgent.class)
                .eq(SyAgent::getAgentCode, agentCode)
                .eq(SyAgent::getTenantId, tenantId)
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public List<SyAgent> listByOwner(Long tenantId, Long ownerUserId) {
        return list(Wrappers.lambdaQuery(SyAgent.class)
                .eq(SyAgent::getTenantId, tenantId)
                .eq(SyAgent::getOwnerUserId, ownerUserId)
                .orderByDesc(SyAgent::getUpdateTime)
                .orderByDesc(SyAgent::getId));
    }
}
