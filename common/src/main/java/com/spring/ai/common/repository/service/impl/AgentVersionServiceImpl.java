package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.AgentVersionMapper;
import com.spring.ai.common.repository.enitiy.AgentVersion;
import com.spring.ai.common.repository.service.AgentVersionService;
import com.spring.ai.common.constants.SqlConstants;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AgentVersionServiceImpl extends ServiceImpl<AgentVersionMapper, AgentVersion>
        implements AgentVersionService {

    @Override
    public List<AgentVersion> listByAgentId(Long agentId, Long tenantId) {
        return list(Wrappers.lambdaQuery(AgentVersion.class)
                .eq(AgentVersion::getAgentId, agentId)
                .eq(AgentVersion::getTenantId, tenantId)
                .orderByDesc(AgentVersion::getVersionNo));
    }

    @Override
    public AgentVersion getByAgentIdAndVersionNo(Long agentId, Long tenantId, Integer versionNo) {
        return getOne(Wrappers.lambdaQuery(AgentVersion.class)
                .eq(AgentVersion::getAgentId, agentId)
                .eq(AgentVersion::getTenantId, tenantId)
                .eq(AgentVersion::getVersionNo, versionNo)
                .last(SqlConstants.LIMIT_ONE));
    }
}
