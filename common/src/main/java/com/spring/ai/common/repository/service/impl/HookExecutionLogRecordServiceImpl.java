package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.HookExecutionLogRecordMapper;
import com.spring.ai.common.repository.enitiy.HookExecutionLogRecord;
import com.spring.ai.common.repository.service.HookExecutionLogRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 文件用途：Hook 执行日志服务实现
 */
@Service
public class HookExecutionLogRecordServiceImpl extends ServiceImpl<HookExecutionLogRecordMapper, HookExecutionLogRecord>
        implements HookExecutionLogRecordService {

    @Override
    public List<HookExecutionLogRecord> listByCondition(Long tenantId, Long hookId, String sourceType, Integer successFlag) {
        return list(Wrappers.lambdaQuery(HookExecutionLogRecord.class)
                .eq(HookExecutionLogRecord::getTenantId, tenantId)
                .eq(hookId != null, HookExecutionLogRecord::getHookId, hookId)
                .eq(StringUtils.hasText(sourceType), HookExecutionLogRecord::getSourceType, sourceType == null ? null : sourceType.trim())
                .eq(successFlag != null, HookExecutionLogRecord::getSuccessFlag, successFlag)
                .orderByDesc(HookExecutionLogRecord::getCreateTime)
                .orderByDesc(HookExecutionLogRecord::getId));
    }

    @Override
    public List<HookExecutionLogRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(HookExecutionLogRecord.class)
                .eq(HookExecutionLogRecord::getTenantId, tenantId)
                .orderByDesc(HookExecutionLogRecord::getCreateTime)
                .orderByDesc(HookExecutionLogRecord::getId));
    }
}
