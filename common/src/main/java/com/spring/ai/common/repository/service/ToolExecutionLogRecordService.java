package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.ToolExecutionLogRecord;
import java.util.List;

/**
 * 文件用途：工具执行日志服务接口
 */
public interface ToolExecutionLogRecordService extends IService<ToolExecutionLogRecord> {

    /**
     * 按工具查询执行日志。
     */
    List<ToolExecutionLogRecord> listByToolId(Long toolId, Long tenantId);

    /**
     * 按筛选条件查询工具日志。
     */
    List<ToolExecutionLogRecord> listByCondition(Long tenantId, Long toolId, String sourceType, Integer successFlag);
}
