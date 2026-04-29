package com.spring.ai.mcp.application.manager;

import com.spring.ai.common.repository.enitiy.McpExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.common.repository.service.McpExecutionLogRecordService;
import com.spring.ai.mcp.config.McpManagementConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：MCP 调用日志管理器
 * 核心职责：统一记录调试与运行时工具调用日志，避免各内置服务重复拼装日志对象
 */
@Component
public class McpInvocationLogManager {

    @Resource
    private McpSupportManager mcpSupportManager;

    @Resource
    private McpExecutionLogRecordService mcpExecutionLogRecordService;

    /**
     * 保存 MCP 调用日志。
     */
    public void saveLog(
            McpServerRecord record,
            String toolName,
            String requestPayloadJson,
            String responsePayloadJson,
            String failureReason,
            long elapsedMs,
            String sourceType,
            Integer successFlag
    ) {
        McpExecutionLogRecord logRecord = new McpExecutionLogRecord();
        logRecord.setServerId(record.getId());
        logRecord.setServerCode(record.getServerCode());
        logRecord.setServerName(record.getServerName());
        logRecord.setToolName(toolName);
        logRecord.setTenantId(record.getTenantId());
        logRecord.setSourceType(StringUtils.hasText(sourceType) ? sourceType.trim() : McpManagementConstants.LOG_SOURCE_RUNTIME);
        logRecord.setRequestPayloadJson(requestPayloadJson);
        logRecord.setResponsePayloadJson(responsePayloadJson);
        logRecord.setExecuteStatus(Integer.valueOf(1).equals(successFlag)
                ? McpManagementConstants.EXECUTE_STATUS_SUCCESS
                : McpManagementConstants.EXECUTE_STATUS_FAILED);
        logRecord.setSuccessFlag(successFlag);
        logRecord.setElapsedMs(elapsedMs);
        logRecord.setFailureReason(failureReason);
        logRecord.setOperatorUserId(mcpSupportManager.getCurrentUserId());
        logRecord.setOperatorUserName(mcpSupportManager.getCurrentUserName());
        mcpExecutionLogRecordService.save(logRecord);
    }
}
