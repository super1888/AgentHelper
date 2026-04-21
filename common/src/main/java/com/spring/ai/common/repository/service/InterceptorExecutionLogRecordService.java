package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.InterceptorExecutionLogRecord;
import java.util.List;

/**
 * 文件用途：Interceptor 执行日志服务接口
 */
public interface InterceptorExecutionLogRecordService extends IService<InterceptorExecutionLogRecord> {

    /**
     * 查询拦截器日志列表。
     */
    List<InterceptorExecutionLogRecord> listByCondition(Long tenantId, Long interceptorId, String sourceType, Integer successFlag);

    /**
     * 查询租户日志。
     */
    List<InterceptorExecutionLogRecord> listByTenantId(Long tenantId);
}
