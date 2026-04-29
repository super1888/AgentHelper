package com.spring.ai.mcp.application.assembler;

import com.spring.ai.common.repository.enitiy.McpExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.mcp.domain.dto.McpServerExtDTO;
import com.spring.ai.mcp.domain.request.McpSaveRequest;
import com.spring.ai.mcp.domain.response.McpCatalogResponse;
import com.spring.ai.mcp.domain.response.McpDebugResponse;
import com.spring.ai.mcp.domain.response.McpExecutionLogResponse;
import com.spring.ai.mcp.domain.response.McpResponse;
import java.time.ZoneId;
import java.util.List;

/**
 * 文件用途：MCP 模块对象组装器
 */
public final class McpAssembler {

    private McpAssembler() {
    }

    public static McpServerRecord toCreateRecord(
            McpSaveRequest request,
            Long tenantId,
            Long currentUserId,
            String currentUserName
    ) {
        McpServerRecord record = new McpServerRecord();
        mergeRecord(record, request);
        record.setTenantId(tenantId);
        record.setOwnerUserId(currentUserId);
        record.setOwnerUserName(currentUserName);
        record.setDeletedFlag(0);
        return record;
    }

    public static void mergeRecord(McpServerRecord record, McpSaveRequest request) {
        record.setServerCode(trim(request.getServerCode()));
        record.setServerName(trim(request.getServerName()));
        record.setDescription(trimToNull(request.getDescription()));
        record.setServerType(trim(request.getServerType()));
        record.setTransportType(trim(request.getTransportType()));
        record.setServerStatus(trim(request.getServerStatus()));
        record.setRiskLevel(trim(request.getRiskLevel()));
        record.setSortWeight(request.getSortWeight());
        record.setTimeoutMs(request.getTimeoutMs());
        record.setAuthRequired(request.getAuthRequired());
        record.setBuiltinServerKey(trimToNull(request.getBuiltinServerKey()));
        record.setEndpointUrl(trimToNull(request.getEndpointUrl()));
        record.setRemark(trimToNull(request.getRemark()));
    }

    public static McpResponse toResponse(McpServerRecord record, McpServerExtDTO ext, Integer logCount) {
        return McpResponse.builder()
                .serverId(record.getId())
                .serverCode(record.getServerCode())
                .serverName(record.getServerName())
                .description(record.getDescription())
                .serverType(record.getServerType())
                .transportType(record.getTransportType())
                .serverStatus(record.getServerStatus())
                .publishStatus(record.getPublishStatus())
                .riskLevel(record.getRiskLevel())
                .sortWeight(record.getSortWeight())
                .timeoutMs(record.getTimeoutMs())
                .authRequired(record.getAuthRequired())
                .builtinServerKey(record.getBuiltinServerKey())
                .endpointUrl(record.getEndpointUrl())
                .tags(ext == null ? List.of() : ext.getTags())
                .runtimeConfigJson(ext == null ? null : ext.getRuntimeConfigJson())
                .authConfigJson(ext == null ? null : ext.getAuthConfigJson())
                .testPayloadJson(ext == null ? null : ext.getTestPayloadJson())
                .toolPromptHint(ext == null ? null : ext.getToolPromptHint())
                .remark(record.getRemark())
                .logCount(logCount)
                .build();
    }

    public static McpExecutionLogResponse toLogResponse(McpExecutionLogRecord record) {
        return McpExecutionLogResponse.builder()
                .logId(record.getId())
                .serverId(record.getServerId())
                .serverCode(record.getServerCode())
                .serverName(record.getServerName())
                .toolName(record.getToolName())
                .sourceType(record.getSourceType())
                .executeStatus(record.getExecuteStatus())
                .successFlag(record.getSuccessFlag())
                .requestPayloadJson(record.getRequestPayloadJson())
                .responsePayloadJson(record.getResponsePayloadJson())
                .failureReason(record.getFailureReason())
                .elapsedMs(record.getElapsedMs())
                .createTime(record.getCreateTime() == null
                        ? null
                        : record.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .build();
    }

    public static McpDebugResponse toDebugResponse(
            McpServerRecord record,
            Integer successFlag,
            String requestPayloadJson,
            String responsePayloadJson,
            String failureReason,
            long elapsedMs
    ) {
        return McpDebugResponse.builder()
                .serverId(record.getId())
                .serverCode(record.getServerCode())
                .serverName(record.getServerName())
                .successFlag(successFlag)
                .requestPayloadJson(requestPayloadJson)
                .responsePayloadJson(responsePayloadJson)
                .failureReason(failureReason)
                .elapsedMs(elapsedMs)
                .build();
    }

    public static McpCatalogResponse toCatalogResponse(
            String builtinServerKey,
            String serverName,
            String description,
            String serverType,
            String transportType,
            String riskLevel,
            Integer authRequired,
            List<String> exposedToolNames,
            String defaultRuntimeConfigJson,
            String defaultAuthConfigJson,
            String defaultTestPayloadJson,
            String toolPromptHint
    ) {
        return McpCatalogResponse.builder()
                .builtinServerKey(builtinServerKey)
                .serverName(serverName)
                .description(description)
                .serverType(serverType)
                .transportType(transportType)
                .riskLevel(riskLevel)
                .authRequired(authRequired)
                .exposedToolNames(exposedToolNames)
                .defaultRuntimeConfigJson(defaultRuntimeConfigJson)
                .defaultAuthConfigJson(defaultAuthConfigJson)
                .defaultTestPayloadJson(defaultTestPayloadJson)
                .toolPromptHint(toolPromptHint)
                .build();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
