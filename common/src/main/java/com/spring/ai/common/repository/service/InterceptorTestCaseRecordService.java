package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.InterceptorTestCaseRecord;
import java.util.List;

/**
 * 文件用途：Interceptor 测试用例服务接口
 */
public interface InterceptorTestCaseRecordService extends IService<InterceptorTestCaseRecord> {

    /**
     * 查询拦截器测试用例列表。
     */
    List<InterceptorTestCaseRecord> listByInterceptorId(Long interceptorId, Long tenantId);
}
