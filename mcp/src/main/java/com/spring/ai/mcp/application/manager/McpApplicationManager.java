package com.spring.ai.mcp.application.manager;

import com.spring.ai.common.exception.BusinessExceptions;
import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.common.repository.service.McpExecutionLogRecordService;
import com.spring.ai.common.repository.service.McpServerRecordService;
import com.spring.ai.mcp.application.assembler.McpAssembler;
import com.spring.ai.mcp.application.model.McpInvocationResult;
import com.spring.ai.mcp.application.registry.McpCatalogRegistry;
import com.spring.ai.mcp.config.McpManagementConstants;
import com.spring.ai.mcp.domain.dto.McpServerExtDTO;
import com.spring.ai.mcp.domain.request.McpDebugRequest;
import com.spring.ai.mcp.domain.request.McpLogQueryRequest;
import com.spring.ai.mcp.domain.request.McpSaveRequest;
import com.spring.ai.mcp.domain.response.McpCatalogResponse;
import com.spring.ai.mcp.domain.response.McpDebugResponse;
import com.spring.ai.mcp.domain.response.McpExecutionLogResponse;
import com.spring.ai.mcp.domain.response.McpResponse;
import com.spring.ai.mcp.domain.response.McpStatisticsResponse;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文件用途：MCP 管理应用编排组件
 * 核心职责：提供 MCP 服务目录、配置、发布上下线、在线调试和日志查询能力
 */
@Component
public class McpApplicationManager {

    @Resource
    private McpServerRecordService mcpServerRecordService;

    @Resource
    private McpExecutionLogRecordService mcpExecutionLogRecordService;

    @Resource
    private McpSupportManager mcpSupportManager;

    @Resource
    private McpCatalogRegistry mcpCatalogRegistry;

    @Resource
    private McpRuntimeManager mcpRuntimeManager;

    /**
     * 查询当前租户下的 MCP 服务列表。
     */
    public List<McpResponse> listServers() {
        return mcpServerRecordService.listByTenantId(mcpSupportManager.getCurrentTenantId())
                .stream()
                .map(record -> McpAssembler.toResponse(
                        record,
                        mcpSupportManager.parseExt(record.getExt()),
                        mcpSupportManager.countLogs(record.getId())))
                .toList();
    }

    /**
     * 查询 MCP 服务详情。
     */
    public McpResponse getServerDetail(Long serverId) {
        McpServerRecord record = mcpSupportManager.requireServer(serverId);
        return McpAssembler.toResponse(
                record,
                mcpSupportManager.parseExt(record.getExt()),
                mcpSupportManager.countLogs(record.getId()));
    }

    /**
     * 查询内置 MCP 服务目录。
     */
    public List<McpCatalogResponse> listCatalog() {
        return mcpCatalogRegistry.listCatalog();
    }

    /**
     * 创建 MCP 服务记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public McpResponse createServer(McpSaveRequest request) {
        validateSaveRequest(request, null);
        McpServerRecord record = McpAssembler.toCreateRecord(
                request,
                mcpSupportManager.getCurrentTenantId(),
                mcpSupportManager.getCurrentUserId(),
                mcpSupportManager.getCurrentUserName()
        );
        record.setPublishStatus(McpManagementConstants.PUBLISH_STATUS_DRAFT);
        record.setExt(mcpSupportManager.buildExtJson(buildExt(request)));
        mcpServerRecordService.save(record);
        return getServerDetail(record.getId());
    }

    /**
     * 更新 MCP 服务记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public McpResponse updateServer(Long serverId, McpSaveRequest request) {
        McpServerRecord record = mcpSupportManager.requireServer(serverId);
        validateSaveRequest(request, serverId);
        McpAssembler.mergeRecord(record, request);
        record.setExt(mcpSupportManager.buildExtJson(buildExt(request)));
        mcpServerRecordService.updateById(record);
        return getServerDetail(serverId);
    }

    /**
     * 删除 MCP 服务记录，使用逻辑删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteServer(Long serverId) {
        McpServerRecord record = mcpSupportManager.requireServer(serverId);
        record.setDeletedFlag(1);
        record.setPublishStatus(McpManagementConstants.PUBLISH_STATUS_OFFLINE);
        mcpServerRecordService.updateById(record);
    }

    /**
     * 发布 MCP 服务。
     */
    @Transactional(rollbackFor = Exception.class)
    public McpResponse publishServer(Long serverId) {
        McpServerRecord record = mcpSupportManager.requireServer(serverId);
        record.setPublishStatus(McpManagementConstants.PUBLISH_STATUS_PUBLISHED);
        record.setServerStatus(McpManagementConstants.SERVER_STATUS_ENABLED);
        mcpServerRecordService.updateById(record);
        return getServerDetail(serverId);
    }

    /**
     * 下线 MCP 服务。
     */
    @Transactional(rollbackFor = Exception.class)
    public McpResponse offlineServer(Long serverId) {
        McpServerRecord record = mcpSupportManager.requireServer(serverId);
        record.setPublishStatus(McpManagementConstants.PUBLISH_STATUS_OFFLINE);
        mcpServerRecordService.updateById(record);
        return getServerDetail(serverId);
    }

    /**
     * 查询 MCP 统计信息。
     */
    public McpStatisticsResponse statistics() {
        List<McpServerRecord> records = mcpServerRecordService.listByTenantId(mcpSupportManager.getCurrentTenantId());
        List<com.spring.ai.common.repository.enitiy.McpExecutionLogRecord> logs = mcpExecutionLogRecordService
                .listByCondition(mcpSupportManager.getCurrentTenantId(), null, null, null);
        int builtinCount = (int) records.stream()
                .filter(record -> McpManagementConstants.SERVER_TYPE_BUILTIN.equals(record.getServerType()))
                .count();
        int highRiskCount = (int) records.stream()
                .filter(record -> McpManagementConstants.RISK_LEVEL_HIGH.equals(record.getRiskLevel()))
                .count();
        int successLogCount = (int) logs.stream()
                .filter(log -> Integer.valueOf(1).equals(log.getSuccessFlag()))
                .count();
        return McpStatisticsResponse.builder()
                .totalCount(records.size())
                .enabledCount((int) records.stream()
                        .filter(record -> McpManagementConstants.SERVER_STATUS_ENABLED.equals(record.getServerStatus()))
                        .count())
                .publishedCount((int) records.stream()
                        .filter(record -> McpManagementConstants.PUBLISH_STATUS_PUBLISHED.equals(record.getPublishStatus()))
                        .count())
                .builtinCount(builtinCount)
                .remoteCount(records.size() - builtinCount)
                .highRiskCount(highRiskCount)
                .totalLogCount(logs.size())
                .successLogCount(successLogCount)
                .failureLogCount(logs.size() - successLogCount)
                .build();
    }

    /**
     * 查询 MCP 执行日志。
     */
    public List<McpExecutionLogResponse> listLogs(McpLogQueryRequest request) {
        Long tenantId = mcpSupportManager.getCurrentTenantId();
        return mcpExecutionLogRecordService
                .listByCondition(
                        tenantId,
                        request == null ? null : request.getServerId(),
                        request == null ? null : request.getSourceType(),
                        request == null ? null : request.getSuccessFlag())
                .stream()
                .map(McpAssembler::toLogResponse)
                .toList();
    }

    /**
     * 在线调试 MCP 服务配置。
     */
    @Transactional(rollbackFor = Exception.class)
    public McpDebugResponse debugServer(McpDebugRequest request) {
        if (request == null || request.getServerId() == null) {
            throw BusinessExceptions.badRequest("serverId 不能为空");
        }
        McpServerRecord record = mcpSupportManager.requireServer(request.getServerId());
        String requestPayloadJson = StringUtils.hasText(request.getRequestPayloadJson())
                ? request.getRequestPayloadJson().trim()
                : mcpSupportManager.parseExt(record.getExt()).getTestPayloadJson();
        Instant start = Instant.now();
        McpInvocationResult result = mcpRuntimeManager.debugInvoke(
                record,
                requestPayloadJson,
                request.getSourceType()
        );
        return McpAssembler.toDebugResponse(
                record,
                1,
                requestPayloadJson,
                result.getResponsePayloadJson(),
                null,
                Duration.between(start, Instant.now()).toMillis()
        );
    }

    private McpServerExtDTO buildExt(McpSaveRequest request) {
        return McpServerExtDTO.builder()
                .tags(request.getTags())
                .runtimeConfigJson(request.getRuntimeConfigJson())
                .authConfigJson(request.getAuthConfigJson())
                .testPayloadJson(request.getTestPayloadJson())
                .toolPromptHint(request.getToolPromptHint())
                .build();
    }

    private void validateSaveRequest(McpSaveRequest request, Long currentServerId) {
        if (request == null) {
            throw BusinessExceptions.badRequest("MCP 服务请求不能为空");
        }
        if (!StringUtils.hasText(request.getServerCode())) {
            throw BusinessExceptions.badRequest("serverCode 不能为空");
        }
        if (!StringUtils.hasText(request.getServerName())) {
            throw BusinessExceptions.badRequest("serverName 不能为空");
        }
        if (!StringUtils.hasText(request.getServerType())) {
            throw BusinessExceptions.badRequest("serverType 不能为空");
        }
        if (!StringUtils.hasText(request.getTransportType())) {
            throw BusinessExceptions.badRequest("transportType 不能为空");
        }
        if (!StringUtils.hasText(request.getServerStatus())) {
            throw BusinessExceptions.badRequest("serverStatus 不能为空");
        }
        if (!StringUtils.hasText(request.getRiskLevel())) {
            throw BusinessExceptions.badRequest("riskLevel 不能为空");
        }
        if (request.getTimeoutMs() == null || request.getTimeoutMs() <= 0) {
            throw BusinessExceptions.badRequest("timeoutMs 必须大于 0");
        }
        mcpSupportManager.validateJsonText(request.getRuntimeConfigJson(), "runtimeConfigJson");
        mcpSupportManager.validateJsonText(request.getAuthConfigJson(), "authConfigJson");
        mcpSupportManager.validateJsonText(request.getTestPayloadJson(), "testPayloadJson");
        McpServerRecord existing = mcpServerRecordService.getByServerCode(mcpSupportManager.getCurrentTenantId(), request.getServerCode());
        if (existing != null && !existing.getId().equals(currentServerId)) {
            throw BusinessExceptions.badRequest("serverCode 已存在: " + request.getServerCode());
        }
        if (McpManagementConstants.SERVER_TYPE_BUILTIN.equalsIgnoreCase(request.getServerType())) {
            if (!StringUtils.hasText(request.getBuiltinServerKey())) {
                throw BusinessExceptions.badRequest("builtinServerKey 不能为空");
            }
            mcpCatalogRegistry.require(request.getBuiltinServerKey().trim());
        }
    }
}
