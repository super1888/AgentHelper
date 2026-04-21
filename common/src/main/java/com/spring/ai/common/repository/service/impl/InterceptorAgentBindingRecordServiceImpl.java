package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.InterceptorAgentBindingRecordMapper;
import com.spring.ai.common.repository.enitiy.InterceptorAgentBindingRecord;
import com.spring.ai.common.repository.service.InterceptorAgentBindingRecordService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 文件用途：Interceptor 绑定服务实现
 */
@Service
public class InterceptorAgentBindingRecordServiceImpl
        extends ServiceImpl<InterceptorAgentBindingRecordMapper, InterceptorAgentBindingRecord>
        implements InterceptorAgentBindingRecordService {

    @Override
    public List<InterceptorAgentBindingRecord> listByInterceptorId(Long interceptorId, Long tenantId) {
        return list(Wrappers.lambdaQuery(InterceptorAgentBindingRecord.class)
                .eq(InterceptorAgentBindingRecord::getInterceptorId, interceptorId)
                .eq(InterceptorAgentBindingRecord::getTenantId, tenantId)
                .orderByAsc(InterceptorAgentBindingRecord::getPriorityNo)
                .orderByDesc(InterceptorAgentBindingRecord::getId));
    }
}
