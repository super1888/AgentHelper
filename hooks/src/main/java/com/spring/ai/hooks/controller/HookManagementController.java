package com.spring.ai.hooks.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.hooks.application.manager.HookApplicationManager;
import com.spring.ai.hooks.domain.request.HookBatchActionRequest;
import com.spring.ai.hooks.domain.request.HookBindingSaveRequest;
import com.spring.ai.hooks.domain.request.HookDebugRequest;
import com.spring.ai.hooks.domain.request.HookLogQueryRequest;
import com.spring.ai.hooks.domain.request.HookSaveRequest;
import com.spring.ai.hooks.domain.request.HookTestCaseSaveRequest;
import com.spring.ai.hooks.domain.request.HookVersionCompareRequest;
import com.spring.ai.hooks.domain.request.HookVersionRollbackRequest;
import com.spring.ai.hooks.domain.response.HookBindingResponse;
import com.spring.ai.hooks.domain.response.HookCatalogResponse;
import com.spring.ai.hooks.domain.response.HookDebugResponse;
import com.spring.ai.hooks.domain.response.HookExecutionLogResponse;
import com.spring.ai.hooks.domain.response.HookResponse;
import com.spring.ai.hooks.domain.response.HookStatisticsResponse;
import com.spring.ai.hooks.domain.response.HookTestCaseResponse;
import com.spring.ai.hooks.domain.response.HookVersionCompareResponse;
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
 * 文件用途：Hook 管理模块对外接口
 * 核心职责：提供 Hook 配置、版本、绑定、调试、测试和日志治理能力
 */
@RestController
@RequestMapping("/hooks")
public class HookManagementController {

    @Resource
    private HookApplicationManager hookApplicationManager;

    @GetMapping
    public ApiResponse<List<HookResponse>> listHooks() {
        return ApiResponse.success(hookApplicationManager.listHooks());
    }

    @GetMapping("/deleted")
    public ApiResponse<List<HookResponse>> listDeletedHooks() {
        return ApiResponse.success(hookApplicationManager.listDeletedHooks());
    }

    @GetMapping("/{hookId}")
    public ApiResponse<HookResponse> getHookDetail(@PathVariable Long hookId) {
        return ApiResponse.success(hookApplicationManager.getHookDetail(hookId));
    }

    @GetMapping("/catalog")
    public ApiResponse<List<HookCatalogResponse>> listCatalog() {
        return ApiResponse.success(hookApplicationManager.listCatalog());
    }

    @PostMapping
    public ApiResponse<HookResponse> createHook(@RequestBody HookSaveRequest request) {
        return ApiResponse.success(hookApplicationManager.createHook(request));
    }

    @PutMapping("/{hookId}")
    public ApiResponse<HookResponse> updateHook(@PathVariable Long hookId, @RequestBody HookSaveRequest request) {
        return ApiResponse.success(hookApplicationManager.updateHook(hookId, request));
    }

    @DeleteMapping("/{hookId}")
    public ApiResponse<Void> deleteHook(@PathVariable Long hookId) {
        hookApplicationManager.deleteHook(hookId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{hookId}/restore")
    public ApiResponse<HookResponse> restoreHook(@PathVariable Long hookId) {
        return ApiResponse.success(hookApplicationManager.restoreHook(hookId));
    }

    @PostMapping("/{hookId}/publish")
    public ApiResponse<HookResponse> publishHook(@PathVariable Long hookId) {
        return ApiResponse.success(hookApplicationManager.publishHook(hookId));
    }

    @PostMapping("/{hookId}/offline")
    public ApiResponse<HookResponse> offlineHook(@PathVariable Long hookId) {
        return ApiResponse.success(hookApplicationManager.offlineHook(hookId));
    }

    @PostMapping("/{hookId}/hot-update")
    public ApiResponse<HookResponse> hotUpdateHook(@PathVariable Long hookId) {
        return ApiResponse.success(hookApplicationManager.hotUpdateHook(hookId));
    }

    @PostMapping("/{hookId}/rollback")
    public ApiResponse<HookResponse> rollbackHook(@PathVariable Long hookId, @RequestBody HookVersionRollbackRequest request) {
        return ApiResponse.success(hookApplicationManager.rollbackHook(hookId, request));
    }

    @PostMapping("/{hookId}/compare")
    public ApiResponse<HookVersionCompareResponse> compareVersions(@PathVariable Long hookId, @RequestBody HookVersionCompareRequest request) {
        return ApiResponse.success(hookApplicationManager.compareVersions(hookId, request));
    }

    @PostMapping("/statistics")
    public ApiResponse<HookStatisticsResponse> statistics() {
        return ApiResponse.success(hookApplicationManager.statistics());
    }

    @PostMapping("/batch/delete")
    public ApiResponse<Void> batchDelete(@RequestBody HookBatchActionRequest request) {
        hookApplicationManager.batchDelete(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/batch/status")
    public ApiResponse<List<HookResponse>> batchUpdateStatus(@RequestBody HookBatchActionRequest request) {
        return ApiResponse.success(hookApplicationManager.batchUpdateStatus(request));
    }

    @PostMapping("/batch/stage")
    public ApiResponse<List<HookResponse>> batchUpdateStage(@RequestBody HookBatchActionRequest request) {
        return ApiResponse.success(hookApplicationManager.batchUpdateStage(request));
    }

    @PostMapping("/batch/risk")
    public ApiResponse<List<HookResponse>> batchUpdateRisk(@RequestBody HookBatchActionRequest request) {
        return ApiResponse.success(hookApplicationManager.batchUpdateRisk(request));
    }

    @PostMapping("/batch/tags")
    public ApiResponse<List<HookResponse>> batchUpdateTags(@RequestBody HookBatchActionRequest request) {
        return ApiResponse.success(hookApplicationManager.batchUpdateTags(request));
    }

    @PostMapping("/batch/publish")
    public ApiResponse<List<HookResponse>> batchPublish(@RequestBody HookBatchActionRequest request) {
        return ApiResponse.success(hookApplicationManager.batchPublish(request));
    }

    @PostMapping("/batch/offline")
    public ApiResponse<List<HookResponse>> batchOffline(@RequestBody HookBatchActionRequest request) {
        return ApiResponse.success(hookApplicationManager.batchOffline(request));
    }

    @PostMapping("/debug")
    public ApiResponse<HookDebugResponse> debugHook(@RequestBody HookDebugRequest request) {
        return ApiResponse.success(hookApplicationManager.debugHook(request));
    }

    @PostMapping("/logs/query")
    public ApiResponse<List<HookExecutionLogResponse>> listLogs(@RequestBody(required = false) HookLogQueryRequest request) {
        return ApiResponse.success(hookApplicationManager.listLogs(request));
    }

    @GetMapping("/{hookId}/test-cases")
    public ApiResponse<List<HookTestCaseResponse>> listTestCases(@PathVariable Long hookId) {
        return ApiResponse.success(hookApplicationManager.listTestCases(hookId));
    }

    @PostMapping("/{hookId}/test-cases")
    public ApiResponse<HookTestCaseResponse> createTestCase(@PathVariable Long hookId, @RequestBody HookTestCaseSaveRequest request) {
        return ApiResponse.success(hookApplicationManager.createTestCase(hookId, request));
    }

    @PutMapping("/test-cases/{testCaseId}")
    public ApiResponse<HookTestCaseResponse> updateTestCase(@PathVariable Long testCaseId, @RequestBody HookTestCaseSaveRequest request) {
        return ApiResponse.success(hookApplicationManager.updateTestCase(testCaseId, request));
    }

    @DeleteMapping("/test-cases/{testCaseId}")
    public ApiResponse<Void> deleteTestCase(@PathVariable Long testCaseId) {
        hookApplicationManager.deleteTestCase(testCaseId);
        return ApiResponse.success(null);
    }

    @PostMapping("/test-cases/{testCaseId}/run")
    public ApiResponse<HookDebugResponse> runTestCase(@PathVariable Long testCaseId) {
        return ApiResponse.success(hookApplicationManager.runTestCase(testCaseId));
    }

    @GetMapping("/{hookId}/bindings")
    public ApiResponse<List<HookBindingResponse>> listBindings(@PathVariable Long hookId) {
        return ApiResponse.success(hookApplicationManager.listBindings(hookId));
    }

    @PostMapping("/{hookId}/bindings")
    public ApiResponse<HookBindingResponse> createBinding(@PathVariable Long hookId, @RequestBody HookBindingSaveRequest request) {
        return ApiResponse.success(hookApplicationManager.createBinding(hookId, request));
    }

    @PutMapping("/bindings/{bindingId}")
    public ApiResponse<HookBindingResponse> updateBinding(@PathVariable Long bindingId, @RequestBody HookBindingSaveRequest request) {
        return ApiResponse.success(hookApplicationManager.updateBinding(bindingId, request));
    }

    @DeleteMapping("/bindings/{bindingId}")
    public ApiResponse<Void> deleteBinding(@PathVariable Long bindingId) {
        hookApplicationManager.deleteBinding(bindingId);
        return ApiResponse.success(null);
    }
}
