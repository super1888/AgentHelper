package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.SkillTestCaseRecordMapper;
import com.spring.ai.common.repository.enitiy.SkillTestCaseRecord;
import com.spring.ai.common.repository.service.SkillTestCaseRecordService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SkillTestCaseRecordServiceImpl extends ServiceImpl<SkillTestCaseRecordMapper, SkillTestCaseRecord>
        implements SkillTestCaseRecordService {

    @Override
    public List<SkillTestCaseRecord> listBySkillId(Long skillId, Long tenantId) {
        return list(Wrappers.lambdaQuery(SkillTestCaseRecord.class)
                .eq(SkillTestCaseRecord::getSkillId, skillId)
                .eq(SkillTestCaseRecord::getTenantId, tenantId)
                .orderByDesc(SkillTestCaseRecord::getUpdateTime)
                .orderByDesc(SkillTestCaseRecord::getId));
    }
}
