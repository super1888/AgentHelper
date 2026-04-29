package com.spring.ai.common.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.spring.ai.common.repository.enitiy.McpExecutionLogRecord;
import java.util.List;

/**
 * 文件用途：MCP 执行日志服务接口
 */
public interface McpExecutionLogRecordService extends IService<McpExecutionLogRecord> {

    /**
     * 按服务查询执行日志。
     */
    List<McpExecutionLogRecord> listByServerId(Long serverId, Long tenantId);

    /**
     * 按筛选条件查询 MCP 日志。
     */
    List<McpExecutionLogRecord> listByCondition(Long tenantId, Long serverId, String sourceType, Integer successFlag);
}
