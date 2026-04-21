package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.HookVersionRecordMapper;
import com.spring.ai.common.repository.enitiy.HookVersionRecord;
import com.spring.ai.common.repository.service.HookVersionRecordService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 文件用途：Hook 版本表服务实现
 */
@Service
public class HookVersionRecordServiceImpl extends ServiceImpl<HookVersionRecordMapper, HookVersionRecord>
        implements HookVersionRecordService {

    @Override
    public List<HookVersionRecord> listByHookId(Long hookId, Long tenantId) {
        return list(Wrappers.lambdaQuery(HookVersionRecord.class)
                .eq(HookVersionRecord::getTenantId, tenantId)
                .eq(HookVersionRecord::getHookId, hookId)
                .orderByDesc(HookVersionRecord::getVersionNo)
                .orderByDesc(HookVersionRecord::getId));
    }

    @Override
    public HookVersionRecord getByHookIdAndVersionNo(Long hookId, Long tenantId, Integer versionNo) {
        return getOne(Wrappers.lambdaQuery(HookVersionRecord.class)
                .eq(HookVersionRecord::getTenantId, tenantId)
                .eq(HookVersionRecord::getHookId, hookId)
                .eq(HookVersionRecord::getVersionNo, versionNo)
                .last(SqlConstants.LIMIT_ONE));
    }
}
