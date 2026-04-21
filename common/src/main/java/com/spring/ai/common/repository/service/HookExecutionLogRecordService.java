package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.HookExecutionLogRecord;
import java.util.List;

/**
 * 文件用途：Hook 执行日志服务接口
 */
public interface HookExecutionLogRecordService extends IService<HookExecutionLogRecord> {

    /**
     * 查询 Hook 日志列表。
     */
    List<HookExecutionLogRecord> listByCondition(Long tenantId, Long hookId, String sourceType, Integer successFlag);

    /**
     * 查询租户日志。
     */
    List<HookExecutionLogRecord> listByTenantId(Long tenantId);
}
