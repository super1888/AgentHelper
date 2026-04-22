package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.A2aTaskRecordMapper;
import com.spring.ai.common.repository.enitiy.A2aTaskRecord;
import com.spring.ai.common.repository.service.A2aTaskRecordService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class A2aTaskRecordServiceImpl extends ServiceImpl<A2aTaskRecordMapper, A2aTaskRecord>
        implements A2aTaskRecordService {

    @Override
    public List<A2aTaskRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(A2aTaskRecord.class)
                .eq(A2aTaskRecord::getTenantId, tenantId)
                .orderByDesc(A2aTaskRecord::getCreateTime)
                .orderByDesc(A2aTaskRecord::getId));
    }
}
