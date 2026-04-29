package com.spring.ai.mcp.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.mcp.application.manager.McpApplicationManager;
import com.spring.ai.mcp.domain.request.McpDebugRequest;
import com.spring.ai.mcp.domain.request.McpLogQueryRequest;
import com.spring.ai.mcp.domain.request.McpSaveRequest;
import com.spring.ai.mcp.domain.response.McpCatalogResponse;
import com.spring.ai.mcp.domain.response.McpDebugResponse;
import com.spring.ai.mcp.domain.response.McpExecutionLogResponse;
import com.spring.ai.mcp.domain.response.McpResponse;
import com.spring.ai.mcp.domain.response.McpStatisticsResponse;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件用途：MCP 服务管理模块对外接口
 * 核心职责：提供 MCP 服务目录、发布上下线、调试和日志查询能力
 */
@RestController
@RequestMapping("/mcp/servers")
public class McpManagementController {

    @Resource
    private McpApplicationManager mcpApplicationManager;

    /**
     * 查询 MCP 服务列表。
     */
    @GetMapping
    public ApiResponse<List<McpResponse>> listServers() {
        return ApiResponse.success(mcpApplicationManager.listServers());
    }

    /**
     * 查询 MCP 服务详情。
     */
    @GetMapping("/{serverId}")
    public ApiResponse<McpResponse> getServerDetail(@PathVariable Long serverId) {
        return ApiResponse.success(mcpApplicationManager.getServerDetail(serverId));
    }

    /**
     * 查询内置 MCP 服务目录。
     */
    @GetMapping("/catalog")
    public ApiResponse<List<McpCatalogResponse>> listCatalog() {
        return ApiResponse.success(mcpApplicationManager.listCatalog());
    }

    /**
     * 创建 MCP 服务。
     */
    @PostMapping
    public ApiResponse<McpResponse> createServer(@RequestBody McpSaveRequest request) {
        return ApiResponse.success(mcpApplicationManager.createServer(request));
    }

    /**
     * 更新 MCP 服务。
     */
    @PutMapping("/{serverId}")
    public ApiResponse<McpResponse> updateServer(@PathVariable Long serverId, @RequestBody McpSaveRequest request) {
        return ApiResponse.success(mcpApplicationManager.updateServer(serverId, request));
    }

    /**
     * 删除 MCP 服务。
     */
    @DeleteMapping("/{serverId}")
    public ApiResponse<Void> deleteServer(@PathVariable Long serverId) {
        mcpApplicationManager.deleteServer(serverId);
        return ApiResponse.success(null);
    }

    /**
     * 发布 MCP 服务。
     */
    @PostMapping("/{serverId}/publish")
    public ApiResponse<McpResponse> publishServer(@PathVariable Long serverId) {
        return ApiResponse.success(mcpApplicationManager.publishServer(serverId));
    }

    /**
     * 下线 MCP 服务。
     */
    @PostMapping("/{serverId}/offline")
    public ApiResponse<McpResponse> offlineServer(@PathVariable Long serverId) {
        return ApiResponse.success(mcpApplicationManager.offlineServer(serverId));
    }

    /**
     * 获取 MCP 统计信息。
     */
    @PostMapping("/statistics")
    public ApiResponse<McpStatisticsResponse> statistics() {
        return ApiResponse.success(mcpApplicationManager.statistics());
    }

    /**
     * 在线调试 MCP 服务。
     */
    @PostMapping("/debug")
    public ApiResponse<McpDebugResponse> debugServer(@RequestBody McpDebugRequest request) {
        return ApiResponse.success(mcpApplicationManager.debugServer(request));
    }

    /**
     * 查询 MCP 执行日志。
     */
    @PostMapping("/logs/query")
    public ApiResponse<List<McpExecutionLogResponse>> listLogs(@RequestBody(required = false) McpLogQueryRequest request) {
        return ApiResponse.success(mcpApplicationManager.listLogs(request));
    }
}
