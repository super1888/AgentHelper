package com.spring.ai.mcp.application.manager;

import com.spring.ai.common.repository.enitiy.McpServerRecord;
import com.spring.ai.common.repository.service.McpServerRecordService;
import com.spring.ai.mcp.application.model.McpBuiltinServerDefinition;
import com.spring.ai.mcp.application.model.McpInvocationResult;
import com.spring.ai.mcp.application.registry.McpCatalogRegistry;
import com.spring.ai.mcp.config.McpManagementConstants;
import com.spring.ai.mcp.domain.dto.McpAuthConfigDTO;
import com.spring.ai.mcp.domain.dto.McpServerExtDTO;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 文件用途：MCP 运行时管理器
 * 核心职责：将已发布 MCP 服务转换为 Agent 可消费的 ToolCallback，并负责运行时调试与日志持久化
 */
@Component
public class McpRuntimeManager {

    @Resource
    private McpSupportManager mcpSupportManager;

    @Resource
    private McpCatalogRegistry mcpCatalogRegistry;

    @Resource
    private McpServerRecordService mcpServerRecordService;

    @Resource
    private McpInvocationLogManager mcpInvocationLogManager;

    /**
     * 将 MCP 服务列表转换为 ToolCallback 集合。
     */
    public List<ToolCallback> resolveToolCallbacks(List<String> selectedMcpServerIds) {
        List<Long> serverIds = mcpSupportManager.parseLongIds(selectedMcpServerIds, "selectedMcpServerIds");
        if (CollectionUtils.isEmpty(serverIds)) {
            return List.of();
        }
        List<ToolCallback> callbacks = new ArrayList<>();
        for (Long serverId : serverIds) {
            McpServerRecord record = mcpSupportManager.requireServer(serverId);
            mcpSupportManager.validateBindableServer(record);
            callbacks.addAll(buildToolCallbacks(record));
        }
        return callbacks;
    }

    /**
     * 校验勾选的 MCP 服务是否可挂载。
     */
    public void validateSelectedServers(List<String> selectedMcpServerIds) {
        List<Long> serverIds = mcpSupportManager.parseLongIds(selectedMcpServerIds, "selectedMcpServerIds");
        for (Long serverId : serverIds) {
            McpServerRecord record = mcpSupportManager.requireServer(serverId);
            mcpSupportManager.validateBindableServer(record);
        }
    }

    /**
     * 执行 MCP 调试并持久化日志。
     */
    @Transactional(rollbackFor = Exception.class)
    public McpInvocationResult debugInvoke(McpServerRecord record, String requestPayloadJson, String sourceType) {
        McpServerExtDTO ext = mcpSupportManager.parseExt(record.getExt());
        McpAuthConfigDTO authConfig = mcpSupportManager.parseAuthConfig(ext.getAuthConfigJson());
        Instant start = Instant.now();
        try {
            McpBuiltinServerDefinition definition = mcpCatalogRegistry.require(record.getBuiltinServerKey());
            McpInvocationResult result = definition.debugInvoke(record, ext, authConfig, requestPayloadJson);
            mcpInvocationLogManager.saveLog(
                    record,
                    result.getToolName(),
                    requestPayloadJson,
                    result.getResponsePayloadJson(),
                    null,
                    Duration.between(start, Instant.now()).toMillis(),
                    sourceType,
                    1);
            return result;
        } catch (Exception exception) {
            mcpInvocationLogManager.saveLog(
                    record,
                    null,
                    requestPayloadJson,
                    null,
                    exception.getMessage(),
                    Duration.between(start, Instant.now()).toMillis(),
                    sourceType,
                    0);
            throw exception;
        }
    }

    /**
     * 查询租户下所有已启用服务。
     */
    public List<McpServerRecord> listEnabledServers() {
        return mcpServerRecordService.listByTenantId(mcpSupportManager.getCurrentTenantId())
                .stream()
                .filter(item -> McpManagementConstants.SERVER_STATUS_ENABLED.equals(item.getServerStatus()))
                .filter(item -> McpManagementConstants.PUBLISH_STATUS_PUBLISHED.equals(item.getPublishStatus()))
                .toList();
    }

    private List<ToolCallback> buildToolCallbacks(McpServerRecord record) {
        if (!StringUtils.hasText(record.getBuiltinServerKey())) {
            return List.of();
        }
        McpServerExtDTO ext = mcpSupportManager.parseExt(record.getExt());
        McpBuiltinServerDefinition definition = mcpCatalogRegistry.require(record.getBuiltinServerKey());
        return definition.createToolCallbacks(record, ext);
    }

}
