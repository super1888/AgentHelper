package com.spring.ai.tools.application.manager;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.ToolExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.ToolRecord;
import com.spring.ai.common.repository.service.ToolExecutionLogRecordService;
import com.spring.ai.common.repository.service.ToolRecordService;
import com.spring.ai.tools.application.assmbler.ToolAssembler;
import com.spring.ai.tools.config.ToolManagementConstants;
import com.spring.ai.tools.domain.dto.ToolRecordExtDTO;
import com.spring.ai.tools.domain.request.ToolDebugRequest;
import com.spring.ai.tools.domain.request.ToolLogQueryRequest;
import com.spring.ai.tools.domain.request.ToolSaveRequest;
import com.spring.ai.tools.domain.response.ToolCatalogResponse;
import com.spring.ai.tools.domain.response.ToolDebugResponse;
import com.spring.ai.tools.domain.response.ToolExecutionLogResponse;
import com.spring.ai.tools.domain.response.ToolResponse;
import com.spring.ai.tools.domain.response.ToolStatisticsResponse;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文件用途：工具管理应用编排组件
 * 核心功能：提供工具目录、工具编辑、发布下线、在线调试和日志查询能力
 */
@Component
public class ToolApplicationManager {

    @Resource
    private ToolRecordService toolRecordService;

    @Resource
    private ToolExecutionLogRecordService toolExecutionLogRecordService;

    @Resource
    private ToolSupportManager toolSupportManager;

    @Resource
    private ToolCatalogRegistry toolCatalogRegistry;

    /**
     * 查询当前租户下的工具列表。
     */
    public List<ToolResponse> listTools() {
        return toolRecordService.listByTenantId(toolSupportManager.getCurrentTenantId())
                .stream()
                .map(record -> ToolAssembler.toResponse(
                        record,
                        toolSupportManager.parseExt(record.getExt()),
                        toolSupportManager.countLogs(record.getId())))
                .toList();
    }

    /**
     * 查询工具详情。
     */
    public ToolResponse getToolDetail(Long toolId) {
        ToolRecord record = toolSupportManager.requireTool(toolId);
        return ToolAssembler.toResponse(
                record,
                toolSupportManager.parseExt(record.getExt()),
                toolSupportManager.countLogs(record.getId()));
    }

    /**
     * 查询内置工具目录。
     */
    public List<ToolCatalogResponse> listCatalog() {
        return toolCatalogRegistry.listCatalog();
    }

    /**
     * 创建工具记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public ToolResponse createTool(ToolSaveRequest request) {
        validateSaveRequest(request, null);
        ToolRecord record = ToolAssembler.toCreateRecord(
                request,
                toolSupportManager.getCurrentTenantId(),
                toolSupportManager.getCurrentUserId(),
                toolSupportManager.getCurrentUserName()
        );
        record.setPublishStatus(ToolManagementConstants.PUBLISH_STATUS_DRAFT);
        record.setExt(toolSupportManager.buildExtJson(buildExt(request)));
        toolRecordService.save(record);
        return getToolDetail(record.getId());
    }

    /**
     * 更新工具记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public ToolResponse updateTool(Long toolId, ToolSaveRequest request) {
        ToolRecord record = toolSupportManager.requireTool(toolId);
        validateSaveRequest(request, toolId);
        ToolAssembler.mergeRecord(record, request);
        record.setExt(toolSupportManager.buildExtJson(buildExt(request)));
        toolRecordService.updateById(record);
        return getToolDetail(toolId);
    }

    /**
     * 删除工具记录，使用逻辑删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTool(Long toolId) {
        ToolRecord record = toolSupportManager.requireTool(toolId);
        record.setDeletedFlag(1);
        record.setPublishStatus(ToolManagementConstants.PUBLISH_STATUS_OFFLINE);
        toolRecordService.updateById(record);
    }

    /**
     * 发布工具。
     */
    @Transactional(rollbackFor = Exception.class)
    public ToolResponse publishTool(Long toolId) {
        ToolRecord record = toolSupportManager.requireTool(toolId);
        record.setPublishStatus(ToolManagementConstants.PUBLISH_STATUS_PUBLISHED);
        record.setToolStatus(ToolManagementConstants.TOOL_STATUS_ENABLED);
        toolRecordService.updateById(record);
        return getToolDetail(toolId);
    }

    /**
     * 下线工具。
     */
    @Transactional(rollbackFor = Exception.class)
    public ToolResponse offlineTool(Long toolId) {
        ToolRecord record = toolSupportManager.requireTool(toolId);
        record.setPublishStatus(ToolManagementConstants.PUBLISH_STATUS_OFFLINE);
        toolRecordService.updateById(record);
        return getToolDetail(toolId);
    }

    /**
     * 查询工具统计信息。
     */
    public ToolStatisticsResponse statistics() {
        List<ToolRecord> records = toolRecordService.listByTenantId(toolSupportManager.getCurrentTenantId());
        List<ToolExecutionLogRecord> logs = toolExecutionLogRecordService
                .listByCondition(toolSupportManager.getCurrentTenantId(), null, null, null);
        int builtinCount = (int) records.stream()
                .filter(record -> ToolManagementConstants.SOURCE_TYPE_BUILTIN.equals(record.getSourceType()))
                .count();
        int highRiskCount = (int) records.stream()
                .filter(record -> ToolManagementConstants.RISK_LEVEL_HIGH.equals(record.getRiskLevel()))
                .count();
        int successLogCount = (int) logs.stream()
                .filter(log -> Integer.valueOf(1).equals(log.getSuccessFlag()))
                .count();
        return ToolStatisticsResponse.builder()
                .totalCount(records.size())
                .enabledCount((int) records.stream().filter(record -> ToolManagementConstants.TOOL_STATUS_ENABLED.equals(record.getToolStatus())).count())
                .publishedCount((int) records.stream().filter(record -> ToolManagementConstants.PUBLISH_STATUS_PUBLISHED.equals(record.getPublishStatus())).count())
                .builtinCount(builtinCount)
                .externalCount(records.size() - builtinCount)
                .highRiskCount(highRiskCount)
                .totalLogCount(logs.size())
                .successLogCount(successLogCount)
                .failureLogCount(logs.size() - successLogCount)
                .build();
    }

    /**
     * 查询工具执行日志。
     */
    public List<ToolExecutionLogResponse> listLogs(ToolLogQueryRequest request) {
        Long tenantId = toolSupportManager.getCurrentTenantId();
        return toolExecutionLogRecordService
                .listByCondition(
                        tenantId,
                        request == null ? null : request.getToolId(),
                        request == null ? null : request.getSourceType(),
                        request == null ? null : request.getSuccessFlag())
                .stream()
                .map(ToolAssembler::toLogResponse)
                .toList();
    }

    /**
     * 在线调试工具配置。
     */
    @Transactional(rollbackFor = Exception.class)
    public ToolDebugResponse debugTool(ToolDebugRequest request) {
        if (request == null || request.getToolId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "toolId 不能为空");
        }
        ToolRecord record = toolSupportManager.requireTool(request.getToolId());
        String requestPayloadJson = StringUtils.hasText(request.getRequestPayloadJson())
                ? request.getRequestPayloadJson().trim()
                : toolSupportManager.parseExt(record.getExt()).getTestPayloadJson();
        Instant start = Instant.now();
        try {
            toolSupportManager.validateJsonText(requestPayloadJson, "requestPayloadJson");
            Map<String, Object> responseBody = Map.of(
                    "toolCode", record.getToolCode(),
                    "toolName", record.getToolName(),
                    "sourceType", record.getSourceType(),
                    "riskLevel", record.getRiskLevel(),
                    "executionMode", record.getExecutionMode(),
                    "message", "tool config validation passed"
            );
            String responsePayloadJson = toolSupportManager.toJson(responseBody);
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();
            saveExecutionLog(record, requestPayloadJson, responsePayloadJson, null, elapsedMs, request.getSourceType(), 1);
            return ToolAssembler.toDebugResponse(record, 1, responsePayloadJson, null, elapsedMs, requestPayloadJson);
        } catch (Exception exception) {
            long elapsedMs = Duration.between(start, Instant.now()).toMillis();
            String failureReason = exception instanceof BusinessException
                    ? exception.getMessage()
                    : "tool config validation failed";
            saveExecutionLog(record, requestPayloadJson, null, failureReason, elapsedMs, request.getSourceType(), 0);
            return ToolAssembler.toDebugResponse(record, 0, null, failureReason, elapsedMs, requestPayloadJson);
        }
    }

    private ToolRecordExtDTO buildExt(ToolSaveRequest request) {
        return ToolRecordExtDTO.builder()
                .tags(request.getTags())
                .requestSchemaJson(request.getRequestSchemaJson())
                .authConfigJson(request.getAuthConfigJson())
                .runtimeConfigJson(request.getRuntimeConfigJson())
                .testPayloadJson(request.getTestPayloadJson())
                .build();
    }

    private void validateSaveRequest(ToolSaveRequest request, Long currentToolId) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "工具请求不能为空");
        }
        if (!StringUtils.hasText(request.getToolCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "toolCode 不能为空");
        }
        if (!StringUtils.hasText(request.getToolName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "toolName 不能为空");
        }
        if (!StringUtils.hasText(request.getToolType())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "toolType 不能为空");
        }
        if (!StringUtils.hasText(request.getToolCategory())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "toolCategory 不能为空");
        }
        if (!StringUtils.hasText(request.getSourceType())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "sourceType 不能为空");
        }
        if (!StringUtils.hasText(request.getToolStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "toolStatus 不能为空");
        }
        if (!StringUtils.hasText(request.getRiskLevel())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "riskLevel 不能为空");
        }
        if (!StringUtils.hasText(request.getExecutionMode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "executionMode 不能为空");
        }
        if (request.getTimeoutMs() == null || request.getTimeoutMs() <= 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "timeoutMs 必须大于 0");
        }
        toolSupportManager.validateJsonText(request.getRequestSchemaJson(), "requestSchemaJson");
        toolSupportManager.validateJsonText(request.getAuthConfigJson(), "authConfigJson");
        toolSupportManager.validateJsonText(request.getRuntimeConfigJson(), "runtimeConfigJson");
        toolSupportManager.validateJsonText(request.getTestPayloadJson(), "testPayloadJson");
        ToolRecord existing = toolRecordService.getByToolCode(toolSupportManager.getCurrentTenantId(), request.getToolCode());
        if (existing != null && !existing.getId().equals(currentToolId)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "toolCode 已存在: " + request.getToolCode());
        }
    }

    private void saveExecutionLog(
            ToolRecord record,
            String requestPayloadJson,
            String responsePayloadJson,
            String failureReason,
            long elapsedMs,
            String sourceType,
            Integer successFlag
    ) {
        ToolExecutionLogRecord logRecord = new ToolExecutionLogRecord();
        logRecord.setToolId(record.getId());
        logRecord.setToolCode(record.getToolCode());
        logRecord.setToolName(record.getToolName());
        logRecord.setTenantId(record.getTenantId());
        logRecord.setSourceType(StringUtils.hasText(sourceType) ? sourceType.trim() : ToolManagementConstants.LOG_SOURCE_DEBUG);
        logRecord.setRequestPayloadJson(requestPayloadJson);
        logRecord.setResponsePayloadJson(responsePayloadJson);
        logRecord.setExecuteStatus(Integer.valueOf(1).equals(successFlag)
                ? ToolManagementConstants.EXECUTE_STATUS_SUCCESS
                : ToolManagementConstants.EXECUTE_STATUS_FAILED);
        logRecord.setSuccessFlag(successFlag);
        logRecord.setElapsedMs(elapsedMs);
        logRecord.setFailureReason(failureReason);
        logRecord.setOperatorUserId(toolSupportManager.getCurrentUserId());
        logRecord.setOperatorUserName(toolSupportManager.getCurrentUserName());
        toolExecutionLogRecordService.save(logRecord);
    }
}
