package com.spring.ai.tools.application.assmbler;

import com.spring.ai.common.repository.enitiy.ToolExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.ToolRecord;
import com.spring.ai.tools.domain.dto.ToolRecordExtDTO;
import com.spring.ai.tools.domain.request.ToolSaveRequest;
import com.spring.ai.tools.domain.response.ToolCatalogResponse;
import com.spring.ai.tools.domain.response.ToolDebugResponse;
import com.spring.ai.tools.domain.response.ToolExecutionLogResponse;
import com.spring.ai.tools.domain.response.ToolResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

/**
 * 文件用途：工具管理对象组装器
 * 核心功能：统一处理工具记录、日志和接口响应对象之间的转换
 */
public final class ToolAssembler {

    private ToolAssembler() {
    }

    /**
     * 将请求对象转换为工具实体。
     */
    public static ToolRecord toCreateRecord(
            ToolSaveRequest request,
            Long tenantId,
            Long ownerUserId,
            String ownerUserName
    ) {
        ToolRecord record = new ToolRecord();
        mergeRecord(record, request);
        record.setTenantId(tenantId);
        record.setOwnerUserId(ownerUserId);
        record.setOwnerUserName(ownerUserName);
        record.setDeletedFlag(0);
        return record;
    }

    /**
     * 将请求对象合并到工具实体。
     */
    public static void mergeRecord(ToolRecord record, ToolSaveRequest request) {
        record.setToolCode(request.getToolCode());
        record.setToolName(request.getToolName());
        record.setDescription(request.getDescription());
        record.setToolType(request.getToolType());
        record.setToolCategory(request.getToolCategory());
        record.setSourceType(request.getSourceType());
        record.setToolStatus(request.getToolStatus());
        record.setRiskLevel(request.getRiskLevel());
        record.setExecutionMode(request.getExecutionMode());
        record.setSortWeight(request.getSortWeight());
        record.setTimeoutMs(request.getTimeoutMs());
        record.setAuthRequired(request.getAuthRequired());
        record.setBuiltinToolKey(request.getBuiltinToolKey());
        record.setEndpointUrl(request.getEndpointUrl());
        record.setHttpMethod(request.getHttpMethod());
        record.setRemark(request.getRemark());
    }

    /**
     * 将工具记录转换为响应对象。
     */
    public static ToolResponse toResponse(ToolRecord record, ToolRecordExtDTO ext, Integer logCount) {
        return ToolResponse.builder()
                .id(record.getId())
                .toolCode(record.getToolCode())
                .toolName(record.getToolName())
                .description(record.getDescription())
                .toolType(record.getToolType())
                .toolCategory(record.getToolCategory())
                .sourceType(record.getSourceType())
                .toolStatus(record.getToolStatus())
                .publishStatus(record.getPublishStatus())
                .riskLevel(record.getRiskLevel())
                .executionMode(record.getExecutionMode())
                .sortWeight(record.getSortWeight())
                .timeoutMs(record.getTimeoutMs())
                .authRequired(record.getAuthRequired())
                .builtinToolKey(record.getBuiltinToolKey())
                .endpointUrl(record.getEndpointUrl())
                .httpMethod(record.getHttpMethod())
                .tags(ext == null || ext.getTags() == null ? Collections.emptyList() : ext.getTags())
                .requestSchemaJson(ext == null ? null : ext.getRequestSchemaJson())
                .authConfigJson(ext == null ? null : ext.getAuthConfigJson())
                .runtimeConfigJson(ext == null ? null : ext.getRuntimeConfigJson())
                .testPayloadJson(ext == null ? null : ext.getTestPayloadJson())
                .tenantId(record.getTenantId())
                .ownerUserId(record.getOwnerUserId())
                .ownerUserName(record.getOwnerUserName())
                .logCount(logCount)
                .remark(record.getRemark())
                .createTime(toEpochMilli(record.getCreateTime()))
                .updateTime(toEpochMilli(record.getUpdateTime()))
                .build();
    }

    /**
     * 将日志实体转换为日志响应对象。
     */
    public static ToolExecutionLogResponse toLogResponse(ToolExecutionLogRecord record) {
        return ToolExecutionLogResponse.builder()
                .id(record.getId())
                .toolId(record.getToolId())
                .toolCode(record.getToolCode())
                .toolName(record.getToolName())
                .sourceType(record.getSourceType())
                .requestPayloadJson(record.getRequestPayloadJson())
                .responsePayloadJson(record.getResponsePayloadJson())
                .executeStatus(record.getExecuteStatus())
                .successFlag(record.getSuccessFlag())
                .elapsedMs(record.getElapsedMs())
                .failureReason(record.getFailureReason())
                .operatorUserName(record.getOperatorUserName())
                .createTime(toEpochMilli(record.getCreateTime()))
                .build();
    }

    /**
     * 构建调试响应对象。
     */
    public static ToolDebugResponse toDebugResponse(
            ToolRecord record,
            Integer successFlag,
            String responsePayloadJson,
            String failureReason,
            Long elapsedMs,
            String requestPayloadJson
    ) {
        return ToolDebugResponse.builder()
                .toolId(record.getId())
                .toolCode(record.getToolCode())
                .toolName(record.getToolName())
                .successFlag(successFlag)
                .responseText(Integer.valueOf(1).equals(successFlag) ? "工具配置校验通过" : "工具配置校验失败")
                .failureReason(failureReason)
                .elapsedMs(elapsedMs)
                .requestPayloadJson(requestPayloadJson)
                .responsePayloadJson(responsePayloadJson)
                .build();
    }

    /**
     * 构建内置工具目录响应对象。
     */
    public static ToolCatalogResponse toCatalogResponse(
            String toolKey,
            String toolName,
            String description,
            String toolType,
            String toolCategory,
            String sourceType,
            List<String> tags,
            String defaultRequestSchemaJson,
            String defaultRuntimeConfigJson,
            String defaultTestPayloadJson
    ) {
        return ToolCatalogResponse.builder()
                .toolKey(toolKey)
                .toolName(toolName)
                .description(description)
                .toolType(toolType)
                .toolCategory(toolCategory)
                .sourceType(sourceType)
                .tags(tags)
                .defaultRequestSchemaJson(defaultRequestSchemaJson)
                .defaultRuntimeConfigJson(defaultRuntimeConfigJson)
                .defaultTestPayloadJson(defaultTestPayloadJson)
                .build();
    }

    private static Long toEpochMilli(LocalDateTime time) {
        return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
