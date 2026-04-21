package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.InterceptorTestCaseRecordMapper;
import com.spring.ai.common.repository.enitiy.InterceptorTestCaseRecord;
import com.spring.ai.common.repository.service.InterceptorTestCaseRecordService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 文件用途：Interceptor 测试用例服务实现
 */
@Service
public class InterceptorTestCaseRecordServiceImpl extends ServiceImpl<InterceptorTestCaseRecordMapper, InterceptorTestCaseRecord>
        implements InterceptorTestCaseRecordService {

    @Override
    public List<InterceptorTestCaseRecord> listByInterceptorId(Long interceptorId, Long tenantId) {
        return list(Wrappers.lambdaQuery(InterceptorTestCaseRecord.class)
                .eq(InterceptorTestCaseRecord::getInterceptorId, interceptorId)
                .eq(InterceptorTestCaseRecord::getTenantId, tenantId)
                .orderByDesc(InterceptorTestCaseRecord::getUpdateTime)
                .orderByDesc(InterceptorTestCaseRecord::getId));
    }
}
