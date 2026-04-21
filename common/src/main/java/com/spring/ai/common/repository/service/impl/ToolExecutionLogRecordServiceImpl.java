package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.ToolExecutionLogRecordMapper;
import com.spring.ai.common.repository.enitiy.ToolExecutionLogRecord;
import com.spring.ai.common.repository.service.ToolExecutionLogRecordService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 文件用途：工具执行日志服务实现
 */
@Service
public class ToolExecutionLogRecordServiceImpl extends ServiceImpl<ToolExecutionLogRecordMapper, ToolExecutionLogRecord>
        implements ToolExecutionLogRecordService {

    /**
     * 按工具查询执行日志。
     */
    @Override
    public List<ToolExecutionLogRecord> listByToolId(Long toolId, Long tenantId) {
        return list(Wrappers.lambdaQuery(ToolExecutionLogRecord.class)
                .eq(ToolExecutionLogRecord::getTenantId, tenantId)
                .eq(toolId != null, ToolExecutionLogRecord::getToolId, toolId)
                .orderByDesc(ToolExecutionLogRecord::getCreateTime)
                .orderByDesc(ToolExecutionLogRecord::getId));
    }

    /**
     * 按筛选条件查询工具日志。
     */
    @Override
    public List<ToolExecutionLogRecord> listByCondition(Long tenantId, Long toolId, String sourceType, Integer successFlag) {
        return list(Wrappers.lambdaQuery(ToolExecutionLogRecord.class)
                .eq(ToolExecutionLogRecord::getTenantId, tenantId)
                .eq(toolId != null, ToolExecutionLogRecord::getToolId, toolId)
                .eq(StringUtils.hasText(sourceType), ToolExecutionLogRecord::getSourceType, sourceType == null ? null : sourceType.trim())
                .eq(successFlag != null, ToolExecutionLogRecord::getSuccessFlag, successFlag)
                .orderByDesc(ToolExecutionLogRecord::getCreateTime)
                .orderByDesc(ToolExecutionLogRecord::getId));
    }
}
