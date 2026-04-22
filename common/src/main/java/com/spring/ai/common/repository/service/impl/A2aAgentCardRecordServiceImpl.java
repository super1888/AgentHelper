package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.A2aAgentCardRecordMapper;
import com.spring.ai.common.repository.enitiy.A2aAgentCardRecord;
import com.spring.ai.common.repository.service.A2aAgentCardRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class A2aAgentCardRecordServiceImpl extends ServiceImpl<A2aAgentCardRecordMapper, A2aAgentCardRecord>
        implements A2aAgentCardRecordService {

    @Override
    public List<A2aAgentCardRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(A2aAgentCardRecord.class)
                .eq(A2aAgentCardRecord::getTenantId, tenantId)
                .and(wrapper -> wrapper.ne(A2aAgentCardRecord::getDeletedFlag, 1).or().isNull(A2aAgentCardRecord::getDeletedFlag))
                .orderByDesc(A2aAgentCardRecord::getUpdateTime)
                .orderByDesc(A2aAgentCardRecord::getId));
    }

    @Override
    public List<A2aAgentCardRecord> listDeletedByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(A2aAgentCardRecord.class)
                .eq(A2aAgentCardRecord::getTenantId, tenantId)
                .eq(A2aAgentCardRecord::getDeletedFlag, 1)
                .orderByDesc(A2aAgentCardRecord::getUpdateTime)
                .orderByDesc(A2aAgentCardRecord::getId));
    }

    @Override
    public A2aAgentCardRecord getByAgentCode(Long tenantId, String agentCode) {
        if (tenantId == null || !StringUtils.hasText(agentCode)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(A2aAgentCardRecord.class)
                .eq(A2aAgentCardRecord::getTenantId, tenantId)
                .eq(A2aAgentCardRecord::getAgentCode, agentCode.trim())
                .and(wrapper -> wrapper.ne(A2aAgentCardRecord::getDeletedFlag, 1).or().isNull(A2aAgentCardRecord::getDeletedFlag))
                .last(SqlConstants.LIMIT_ONE));
    }
}
