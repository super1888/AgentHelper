package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.McpExecutionLogRecordMapper;
import com.spring.ai.common.repository.enitiy.McpExecutionLogRecord;
import com.spring.ai.common.repository.service.McpExecutionLogRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 文件用途：MCP 执行日志服务实现
 */
@Service
public class McpExecutionLogRecordServiceImpl extends ServiceImpl<McpExecutionLogRecordMapper, McpExecutionLogRecord>
        implements McpExecutionLogRecordService {

    /**
     * 按服务查询执行日志。
     */
    @Override
    public List<McpExecutionLogRecord> listByServerId(Long serverId, Long tenantId) {
        return list(Wrappers.lambdaQuery(McpExecutionLogRecord.class)
                .eq(McpExecutionLogRecord::getTenantId, tenantId)
                .eq(serverId != null, McpExecutionLogRecord::getServerId, serverId)
                .orderByDesc(McpExecutionLogRecord::getCreateTime)
                .orderByDesc(McpExecutionLogRecord::getId));
    }

    /**
     * 按筛选条件查询 MCP 日志。
     */
    @Override
    public List<McpExecutionLogRecord> listByCondition(Long tenantId, Long serverId, String sourceType, Integer successFlag) {
        return list(Wrappers.lambdaQuery(McpExecutionLogRecord.class)
                .eq(McpExecutionLogRecord::getTenantId, tenantId)
                .eq(serverId != null, McpExecutionLogRecord::getServerId, serverId)
                .eq(StringUtils.hasText(sourceType), McpExecutionLogRecord::getSourceType, sourceType == null ? null : sourceType.trim())
                .eq(successFlag != null, McpExecutionLogRecord::getSuccessFlag, successFlag)
                .orderByDesc(McpExecutionLogRecord::getCreateTime)
                .orderByDesc(McpExecutionLogRecord::getId));
    }
}
