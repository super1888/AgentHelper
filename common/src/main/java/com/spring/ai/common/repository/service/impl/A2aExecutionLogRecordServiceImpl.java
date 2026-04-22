package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.A2aExecutionLogRecordMapper;
import com.spring.ai.common.repository.enitiy.A2aExecutionLogRecord;
import com.spring.ai.common.repository.service.A2aExecutionLogRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class A2aExecutionLogRecordServiceImpl extends ServiceImpl<A2aExecutionLogRecordMapper, A2aExecutionLogRecord>
        implements A2aExecutionLogRecordService {

    @Override
    public List<A2aExecutionLogRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(A2aExecutionLogRecord.class)
                .eq(A2aExecutionLogRecord::getTenantId, tenantId)
                .orderByDesc(A2aExecutionLogRecord::getCreateTime)
                .orderByDesc(A2aExecutionLogRecord::getId));
    }

    @Override
    public List<A2aExecutionLogRecord> listByTaskCode(Long tenantId, String taskCode) {
        return list(Wrappers.lambdaQuery(A2aExecutionLogRecord.class)
                .eq(A2aExecutionLogRecord::getTenantId, tenantId)
                .eq(StringUtils.hasText(taskCode), A2aExecutionLogRecord::getTaskCode, taskCode == null ? null : taskCode.trim())
                .orderByDesc(A2aExecutionLogRecord::getCreateTime)
                .orderByDesc(A2aExecutionLogRecord::getId));
    }
}
