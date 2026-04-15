package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.SyAgentVersionMapper;
import com.spring.ai.common.repository.enitiy.SyAgentVersion;
import com.spring.ai.common.repository.service.SyAgentVersionService;
import com.spring.ai.common.constants.SqlConstants;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SyAgentVersionServiceImpl extends ServiceImpl<SyAgentVersionMapper, SyAgentVersion>
        implements SyAgentVersionService {

    @Override
    public List<SyAgentVersion> listByAgentId(Long agentId, Long tenantId) {
        return list(Wrappers.lambdaQuery(SyAgentVersion.class)
                .eq(SyAgentVersion::getAgentId, agentId)
                .eq(SyAgentVersion::getTenantId, tenantId)
                .orderByDesc(SyAgentVersion::getVersionNo));
    }

    @Override
    public SyAgentVersion getByAgentIdAndVersionNo(Long agentId, Long tenantId, Integer versionNo) {
        return getOne(Wrappers.lambdaQuery(SyAgentVersion.class)
                .eq(SyAgentVersion::getAgentId, agentId)
                .eq(SyAgentVersion::getTenantId, tenantId)
                .eq(SyAgentVersion::getVersionNo, versionNo)
                .last(SqlConstants.LIMIT_ONE));
    }
}
