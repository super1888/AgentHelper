package com.spring.ai.interceptors.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.interceptors.application.manager.InterceptorApplicationManager;
import com.spring.ai.interceptors.domain.request.InterceptorBindingSaveRequest;
import com.spring.ai.interceptors.domain.request.InterceptorDebugRequest;
import com.spring.ai.interceptors.domain.request.InterceptorLogQueryRequest;
import com.spring.ai.interceptors.domain.request.InterceptorSaveRequest;
import com.spring.ai.interceptors.domain.request.InterceptorTestCaseSaveRequest;
import com.spring.ai.interceptors.domain.response.InterceptorBindingResponse;
import com.spring.ai.interceptors.domain.response.InterceptorCatalogResponse;
import com.spring.ai.interceptors.domain.response.InterceptorDebugResponse;
import com.spring.ai.interceptors.domain.response.InterceptorExecutionLogResponse;
import com.spring.ai.interceptors.domain.response.InterceptorResponse;
import com.spring.ai.interceptors.domain.response.InterceptorStatisticsResponse;
import com.spring.ai.interceptors.domain.response.InterceptorTestCaseResponse;
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
 * 文件用途：Interceptor 管理模块对外接口
 * 核心职责：提供拦截器配置、版本、绑定、调试、测试和日志治理能力
 */
@RestController
@RequestMapping("/interceptors")
public class InterceptorManagementController {

    @Resource
    private InterceptorApplicationManager interceptorApplicationManager;

    @GetMapping
    public ApiResponse<List<InterceptorResponse>> listInterceptors() {
        return ApiResponse.success(interceptorApplicationManager.listInterceptors());
    }

    @GetMapping("/deleted")
    public ApiResponse<List<InterceptorResponse>> listDeletedInterceptors() {
        return ApiResponse.success(interceptorApplicationManager.listDeletedInterceptors());
    }

    @GetMapping("/{interceptorId}")
    public ApiResponse<InterceptorResponse> getInterceptorDetail(@PathVariable Long interceptorId) {
        return ApiResponse.success(interceptorApplicationManager.getInterceptorDetail(interceptorId));
    }

    @GetMapping("/catalog")
    public ApiResponse<List<InterceptorCatalogResponse>> listCatalog() {
        return ApiResponse.success(interceptorApplicationManager.listCatalog());
    }

    @PostMapping
    public ApiResponse<InterceptorResponse> createInterceptor(@RequestBody InterceptorSaveRequest request) {
        return ApiResponse.success(interceptorApplicationManager.createInterceptor(request));
    }

    @PutMapping("/{interceptorId}")
    public ApiResponse<InterceptorResponse> updateInterceptor(@PathVariable Long interceptorId, @RequestBody InterceptorSaveRequest request) {
        return ApiResponse.success(interceptorApplicationManager.updateInterceptor(interceptorId, request));
    }

    @DeleteMapping("/{interceptorId}")
    public ApiResponse<Void> deleteInterceptor(@PathVariable Long interceptorId) {
        interceptorApplicationManager.deleteInterceptor(interceptorId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{interceptorId}/restore")
    public ApiResponse<InterceptorResponse> restoreInterceptor(@PathVariable Long interceptorId) {
        return ApiResponse.success(interceptorApplicationManager.restoreInterceptor(interceptorId));
    }

    @PostMapping("/{interceptorId}/publish")
    public ApiResponse<InterceptorResponse> publishInterceptor(@PathVariable Long interceptorId) {
        return ApiResponse.success(interceptorApplicationManager.publishInterceptor(interceptorId));
    }

    @PostMapping("/{interceptorId}/offline")
    public ApiResponse<InterceptorResponse> offlineInterceptor(@PathVariable Long interceptorId) {
        return ApiResponse.success(interceptorApplicationManager.offlineInterceptor(interceptorId));
    }

    @PostMapping("/{interceptorId}/hot-update")
    public ApiResponse<InterceptorResponse> hotUpdateInterceptor(@PathVariable Long interceptorId) {
        return ApiResponse.success(interceptorApplicationManager.hotUpdateInterceptor(interceptorId));
    }

    @PostMapping("/statistics")
    public ApiResponse<InterceptorStatisticsResponse> statistics() {
        return ApiResponse.success(interceptorApplicationManager.statistics());
    }

    @PostMapping("/debug")
    public ApiResponse<InterceptorDebugResponse> debugInterceptor(@RequestBody InterceptorDebugRequest request) {
        return ApiResponse.success(interceptorApplicationManager.debugInterceptor(request));
    }

    @PostMapping("/logs/query")
    public ApiResponse<List<InterceptorExecutionLogResponse>> listLogs(@RequestBody(required = false) InterceptorLogQueryRequest request) {
        return ApiResponse.success(interceptorApplicationManager.listLogs(request));
    }

    @GetMapping("/{interceptorId}/test-cases")
    public ApiResponse<List<InterceptorTestCaseResponse>> listTestCases(@PathVariable Long interceptorId) {
        return ApiResponse.success(interceptorApplicationManager.listTestCases(interceptorId));
    }

    @PostMapping("/{interceptorId}/test-cases")
    public ApiResponse<InterceptorTestCaseResponse> createTestCase(
            @PathVariable Long interceptorId,
            @RequestBody InterceptorTestCaseSaveRequest request
    ) {
        return ApiResponse.success(interceptorApplicationManager.createTestCase(interceptorId, request));
    }

    @PostMapping("/test-cases/{testCaseId}/run")
    public ApiResponse<InterceptorDebugResponse> runTestCase(@PathVariable Long testCaseId) {
        return ApiResponse.success(interceptorApplicationManager.runTestCase(testCaseId));
    }

    @GetMapping("/{interceptorId}/bindings")
    public ApiResponse<List<InterceptorBindingResponse>> listBindings(@PathVariable Long interceptorId) {
        return ApiResponse.success(interceptorApplicationManager.listBindings(interceptorId));
    }

    @PostMapping("/{interceptorId}/bindings")
    public ApiResponse<InterceptorBindingResponse> createBinding(
            @PathVariable Long interceptorId,
            @RequestBody InterceptorBindingSaveRequest request
    ) {
        return ApiResponse.success(interceptorApplicationManager.createBinding(interceptorId, request));
    }
}
