package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.SkillRecordMapper;
import com.spring.ai.common.repository.enitiy.SkillRecord;
import com.spring.ai.common.repository.service.SkillRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SkillRecordServiceImpl extends ServiceImpl<SkillRecordMapper, SkillRecord> implements SkillRecordService {

    @Override
    public List<SkillRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(SkillRecord.class)
                .eq(SkillRecord::getTenantId, tenantId)
                .and(wrapper -> wrapper.ne(SkillRecord::getDeletedFlag, 1).or().isNull(SkillRecord::getDeletedFlag))
                .orderByDesc(SkillRecord::getSortWeight)
                .orderByDesc(SkillRecord::getUpdateTime)
                .orderByDesc(SkillRecord::getId));
    }

    @Override
    public List<SkillRecord> listDeletedByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(SkillRecord.class)
                .eq(SkillRecord::getTenantId, tenantId)
                .eq(SkillRecord::getDeletedFlag, 1)
                .orderByDesc(SkillRecord::getUpdateTime)
                .orderByDesc(SkillRecord::getId));
    }

    @Override
    public SkillRecord getBySkillCode(Long tenantId, String skillCode) {
        if (tenantId == null || !StringUtils.hasText(skillCode)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(SkillRecord.class)
                .eq(SkillRecord::getTenantId, tenantId)
                .eq(SkillRecord::getSkillCode, skillCode.trim())
                .and(wrapper -> wrapper.ne(SkillRecord::getDeletedFlag, 1).or().isNull(SkillRecord::getDeletedFlag))
                .last(SqlConstants.LIMIT_ONE));
    }
}
