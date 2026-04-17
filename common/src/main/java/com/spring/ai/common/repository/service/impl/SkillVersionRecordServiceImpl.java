package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.SkillVersionRecordMapper;
import com.spring.ai.common.repository.enitiy.SkillVersionRecord;
import com.spring.ai.common.repository.service.SkillVersionRecordService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SkillVersionRecordServiceImpl extends ServiceImpl<SkillVersionRecordMapper, SkillVersionRecord>
        implements SkillVersionRecordService {

    @Override
    public List<SkillVersionRecord> listBySkillId(Long skillId, Long tenantId) {
        return list(Wrappers.lambdaQuery(SkillVersionRecord.class)
                .eq(SkillVersionRecord::getSkillId, skillId)
                .eq(SkillVersionRecord::getTenantId, tenantId)
                .orderByDesc(SkillVersionRecord::getVersionNo)
                .orderByDesc(SkillVersionRecord::getId));
    }

    @Override
    public SkillVersionRecord getBySkillIdAndVersionNo(Long skillId, Long tenantId, Integer versionNo) {
        if (skillId == null || tenantId == null || versionNo == null) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(SkillVersionRecord.class)
                .eq(SkillVersionRecord::getSkillId, skillId)
                .eq(SkillVersionRecord::getTenantId, tenantId)
                .eq(SkillVersionRecord::getVersionNo, versionNo)
                .last(SqlConstants.LIMIT_ONE));
    }
}
