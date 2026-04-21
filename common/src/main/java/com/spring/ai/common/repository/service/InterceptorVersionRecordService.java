package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.InterceptorVersionRecord;
import java.util.List;

/**
 * 文件用途：Interceptor 版本表服务接口
 */
public interface InterceptorVersionRecordService extends IService<InterceptorVersionRecord> {

    /**
     * 查询指定拦截器的版本列表。
     */
    List<InterceptorVersionRecord> listByInterceptorId(Long interceptorId, Long tenantId);

    /**
     * 查询指定版本。
     */
    InterceptorVersionRecord getByInterceptorIdAndVersionNo(Long interceptorId, Long tenantId, Integer versionNo);
}
