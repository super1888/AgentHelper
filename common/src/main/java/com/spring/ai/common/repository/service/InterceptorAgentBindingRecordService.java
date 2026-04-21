package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.InterceptorAgentBindingRecord;
import java.util.List;

/**
 * 文件用途：Interceptor 绑定服务接口
 */
public interface InterceptorAgentBindingRecordService extends IService<InterceptorAgentBindingRecord> {

    /**
     * 查询拦截器绑定列表。
     */
    List<InterceptorAgentBindingRecord> listByInterceptorId(Long interceptorId, Long tenantId);
}
