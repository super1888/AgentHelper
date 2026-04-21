package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.InterceptorRecordMapper;
import com.spring.ai.common.repository.enitiy.InterceptorRecord;
import com.spring.ai.common.repository.service.InterceptorRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 文件用途：Interceptor 主表服务实现
 */
@Service
public class InterceptorRecordServiceImpl extends ServiceImpl<InterceptorRecordMapper, InterceptorRecord>
        implements InterceptorRecordService {

    @Override
    public List<InterceptorRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(InterceptorRecord.class)
                .eq(InterceptorRecord::getTenantId, tenantId)
                .and(wrapper -> wrapper.ne(InterceptorRecord::getDeletedFlag, 1).or().isNull(InterceptorRecord::getDeletedFlag))
                .orderByDesc(InterceptorRecord::getSortWeight)
                .orderByDesc(InterceptorRecord::getUpdateTime)
                .orderByDesc(InterceptorRecord::getId));
    }

    @Override
    public List<InterceptorRecord> listDeletedByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(InterceptorRecord.class)
                .eq(InterceptorRecord::getTenantId, tenantId)
                .eq(InterceptorRecord::getDeletedFlag, 1)
                .orderByDesc(InterceptorRecord::getUpdateTime)
                .orderByDesc(InterceptorRecord::getId));
    }

    @Override
    public InterceptorRecord getByInterceptorCode(Long tenantId, String interceptorCode) {
        if (tenantId == null || !StringUtils.hasText(interceptorCode)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(InterceptorRecord.class)
                .eq(InterceptorRecord::getTenantId, tenantId)
                .eq(InterceptorRecord::getInterceptorCode, interceptorCode.trim())
                .and(wrapper -> wrapper.ne(InterceptorRecord::getDeletedFlag, 1).or().isNull(InterceptorRecord::getDeletedFlag))
                .last(SqlConstants.LIMIT_ONE));
    }
}
