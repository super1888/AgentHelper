package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.InterceptorExecutionLogRecordMapper;
import com.spring.ai.common.repository.enitiy.InterceptorExecutionLogRecord;
import com.spring.ai.common.repository.service.InterceptorExecutionLogRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 文件用途：Interceptor 执行日志服务实现
 */
@Service
public class InterceptorExecutionLogRecordServiceImpl
        extends ServiceImpl<InterceptorExecutionLogRecordMapper, InterceptorExecutionLogRecord>
        implements InterceptorExecutionLogRecordService {

    @Override
    public List<InterceptorExecutionLogRecord> listByCondition(Long tenantId, Long interceptorId, String sourceType, Integer successFlag) {
        return list(Wrappers.lambdaQuery(InterceptorExecutionLogRecord.class)
                .eq(InterceptorExecutionLogRecord::getTenantId, tenantId)
                .eq(interceptorId != null, InterceptorExecutionLogRecord::getInterceptorId, interceptorId)
                .eq(StringUtils.hasText(sourceType), InterceptorExecutionLogRecord::getSourceType, sourceType == null ? null : sourceType.trim())
                .eq(successFlag != null, InterceptorExecutionLogRecord::getSuccessFlag, successFlag)
                .orderByDesc(InterceptorExecutionLogRecord::getCreateTime)
                .orderByDesc(InterceptorExecutionLogRecord::getId));
    }

    @Override
    public List<InterceptorExecutionLogRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(InterceptorExecutionLogRecord.class)
                .eq(InterceptorExecutionLogRecord::getTenantId, tenantId)
                .orderByDesc(InterceptorExecutionLogRecord::getCreateTime)
                .orderByDesc(InterceptorExecutionLogRecord::getId));
    }
}
