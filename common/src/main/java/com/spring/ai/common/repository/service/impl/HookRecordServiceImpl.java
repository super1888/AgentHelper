package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.HookRecordMapper;
import com.spring.ai.common.repository.enitiy.HookRecord;
import com.spring.ai.common.repository.service.HookRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 文件用途：Hook 主表服务实现
 */
@Service
public class HookRecordServiceImpl extends ServiceImpl<HookRecordMapper, HookRecord> implements HookRecordService {

    @Override
    public List<HookRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(HookRecord.class)
                .eq(HookRecord::getTenantId, tenantId)
                .and(wrapper -> wrapper.ne(HookRecord::getDeletedFlag, 1).or().isNull(HookRecord::getDeletedFlag))
                .orderByDesc(HookRecord::getSortWeight)
                .orderByDesc(HookRecord::getUpdateTime)
                .orderByDesc(HookRecord::getId));
    }

    @Override
    public List<HookRecord> listDeletedByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(HookRecord.class)
                .eq(HookRecord::getTenantId, tenantId)
                .eq(HookRecord::getDeletedFlag, 1)
                .orderByDesc(HookRecord::getUpdateTime)
                .orderByDesc(HookRecord::getId));
    }

    @Override
    public HookRecord getByHookCode(Long tenantId, String hookCode) {
        if (tenantId == null || !StringUtils.hasText(hookCode)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(HookRecord.class)
                .eq(HookRecord::getTenantId, tenantId)
                .eq(HookRecord::getHookCode, hookCode.trim())
                .and(wrapper -> wrapper.ne(HookRecord::getDeletedFlag, 1).or().isNull(HookRecord::getDeletedFlag))
                .last(SqlConstants.LIMIT_ONE));
    }
}
