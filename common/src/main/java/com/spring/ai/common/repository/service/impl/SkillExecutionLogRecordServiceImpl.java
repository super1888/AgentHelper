package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.SkillExecutionLogRecordMapper;
import com.spring.ai.common.repository.enitiy.SkillExecutionLogRecord;
import com.spring.ai.common.repository.service.SkillExecutionLogRecordService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SkillExecutionLogRecordServiceImpl
        extends ServiceImpl<SkillExecutionLogRecordMapper, SkillExecutionLogRecord>
        implements SkillExecutionLogRecordService {

    @Override
    public List<SkillExecutionLogRecord> listBySkillId(Long skillId, Long tenantId) {
        return list(Wrappers.lambdaQuery(SkillExecutionLogRecord.class)
                .eq(SkillExecutionLogRecord::getSkillId, skillId)
                .eq(SkillExecutionLogRecord::getTenantId, tenantId)
                .orderByDesc(SkillExecutionLogRecord::getCreateTime)
                .orderByDesc(SkillExecutionLogRecord::getId));
    }

    @Override
    public List<SkillExecutionLogRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(SkillExecutionLogRecord.class)
                .eq(SkillExecutionLogRecord::getTenantId, tenantId)
                .orderByDesc(SkillExecutionLogRecord::getCreateTime)
                .orderByDesc(SkillExecutionLogRecord::getId));
    }
}
