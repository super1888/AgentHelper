package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.InterceptorRecord;
import java.util.List;

/**
 * 文件用途：Interceptor 主表服务接口
 */
public interface InterceptorRecordService extends IService<InterceptorRecord> {

    /**
     * 查询租户下有效拦截器列表。
     */
    List<InterceptorRecord> listByTenantId(Long tenantId);

    /**
     * 查询租户下已删除拦截器列表。
     */
    List<InterceptorRecord> listDeletedByTenantId(Long tenantId);

    /**
     * 按拦截器编码查询记录。
     */
    InterceptorRecord getByInterceptorCode(Long tenantId, String interceptorCode);
}
