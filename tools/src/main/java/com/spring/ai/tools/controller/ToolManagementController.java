package com.spring.ai.tools.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.tools.application.manager.ToolApplicationManager;
import com.spring.ai.tools.domain.request.ToolDebugRequest;
import com.spring.ai.tools.domain.request.ToolLogQueryRequest;
import com.spring.ai.tools.domain.request.ToolSaveRequest;
import com.spring.ai.tools.domain.response.ToolCatalogResponse;
import com.spring.ai.tools.domain.response.ToolDebugResponse;
import com.spring.ai.tools.domain.response.ToolExecutionLogResponse;
import com.spring.ai.tools.domain.response.ToolResponse;
import com.spring.ai.tools.domain.response.ToolStatisticsResponse;
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
 * 文件用途：工具管理模块对外接口
 * 核心功能：提供工具目录、工具编辑、发布下线、调试校验和日志查询能力
 */
@RestController
@RequestMapping("/tools")
public class ToolManagementController {

    @Resource
    private ToolApplicationManager toolApplicationManager;

    /**
     * 查询工具列表。
     */
    @GetMapping
    public ApiResponse<List<ToolResponse>> listTools() {
        return ApiResponse.success(toolApplicationManager.listTools());
    }

    /**
     * 查询工具详情。
     */
    @GetMapping("/{toolId}")
    public ApiResponse<ToolResponse> getToolDetail(@PathVariable Long toolId) {
        return ApiResponse.success(toolApplicationManager.getToolDetail(toolId));
    }

    /**
     * 查询内置工具目录。
     */
    @GetMapping("/catalog")
    public ApiResponse<List<ToolCatalogResponse>> listCatalog() {
        return ApiResponse.success(toolApplicationManager.listCatalog());
    }

    /**
     * 创建工具。
     */
    @PostMapping
    public ApiResponse<ToolResponse> createTool(@RequestBody ToolSaveRequest request) {
        return ApiResponse.success(toolApplicationManager.createTool(request));
    }

    /**
     * 更新工具。
     */
    @PutMapping("/{toolId}")
    public ApiResponse<ToolResponse> updateTool(@PathVariable Long toolId, @RequestBody ToolSaveRequest request) {
        return ApiResponse.success(toolApplicationManager.updateTool(toolId, request));
    }

    /**
     * 删除工具。
     */
    @DeleteMapping("/{toolId}")
    public ApiResponse<Void> deleteTool(@PathVariable Long toolId) {
        toolApplicationManager.deleteTool(toolId);
        return ApiResponse.success(null);
    }

    /**
     * 发布工具。
     */
    @PostMapping("/{toolId}/publish")
    public ApiResponse<ToolResponse> publishTool(@PathVariable Long toolId) {
        return ApiResponse.success(toolApplicationManager.publishTool(toolId));
    }

    /**
     * 下线工具。
     */
    @PostMapping("/{toolId}/offline")
    public ApiResponse<ToolResponse> offlineTool(@PathVariable Long toolId) {
        return ApiResponse.success(toolApplicationManager.offlineTool(toolId));
    }

    /**
     * 获取工具统计信息。
     */
    @PostMapping("/statistics")
    public ApiResponse<ToolStatisticsResponse> statistics() {
        return ApiResponse.success(toolApplicationManager.statistics());
    }

    /**
     * 在线调试工具。
     */
    @PostMapping("/debug")
    public ApiResponse<ToolDebugResponse> debugTool(@RequestBody ToolDebugRequest request) {
        return ApiResponse.success(toolApplicationManager.debugTool(request));
    }

    /**
     * 查询工具执行日志。
     */
    @PostMapping("/logs/query")
    public ApiResponse<List<ToolExecutionLogResponse>> listLogs(@RequestBody(required = false) ToolLogQueryRequest request) {
        return ApiResponse.success(toolApplicationManager.listLogs(request));
    }
}
