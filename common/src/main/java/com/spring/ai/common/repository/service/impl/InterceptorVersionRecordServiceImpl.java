package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.InterceptorVersionRecordMapper;
import com.spring.ai.common.repository.enitiy.InterceptorVersionRecord;
import com.spring.ai.common.repository.service.InterceptorVersionRecordService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 文件用途：Interceptor 版本表服务实现
 */
@Service
public class InterceptorVersionRecordServiceImpl extends ServiceImpl<InterceptorVersionRecordMapper, InterceptorVersionRecord>
        implements InterceptorVersionRecordService {

    @Override
    public List<InterceptorVersionRecord> listByInterceptorId(Long interceptorId, Long tenantId) {
        return list(Wrappers.lambdaQuery(InterceptorVersionRecord.class)
                .eq(InterceptorVersionRecord::getInterceptorId, interceptorId)
                .eq(InterceptorVersionRecord::getTenantId, tenantId)
                .orderByDesc(InterceptorVersionRecord::getVersionNo)
                .orderByDesc(InterceptorVersionRecord::getId));
    }

    @Override
    public InterceptorVersionRecord getByInterceptorIdAndVersionNo(Long interceptorId, Long tenantId, Integer versionNo) {
        return getOne(Wrappers.lambdaQuery(InterceptorVersionRecord.class)
                .eq(InterceptorVersionRecord::getInterceptorId, interceptorId)
                .eq(InterceptorVersionRecord::getTenantId, tenantId)
                .eq(InterceptorVersionRecord::getVersionNo, versionNo)
                .last(SqlConstants.LIMIT_ONE));
    }
}
