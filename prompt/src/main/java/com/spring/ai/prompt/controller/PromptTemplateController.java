package com.spring.ai.prompt.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.prompt.application.manager.PromptTemplateApplicationManager;
import com.spring.ai.prompt.domain.request.PromptTemplateCreateRequest;
import com.spring.ai.prompt.domain.request.PromptTemplateRenderRequest;
import com.spring.ai.prompt.domain.request.PromptTemplateUpdateRequest;
import com.spring.ai.prompt.domain.response.PromptTemplateRenderResponse;
import com.spring.ai.prompt.domain.response.PromptTemplateResponse;
import com.spring.ai.prompt.domain.response.PromptTemplateStatisticsResponse;
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
 * 文件用途：对外暴露提示词模板管理接口。
 * 核心功能：提供模板查询、创建、更新、删除、统计和试渲染入口。
 */
@RestController
@RequestMapping("/promptTemplates")
public class PromptTemplateController {

    @Resource
    private PromptTemplateApplicationManager promptTemplateApplicationManager;

    /**
     * 查询当前租户下的模板列表。
     */
    @GetMapping
    public ApiResponse<List<PromptTemplateResponse>> listTemplates() {
        return ApiResponse.success(promptTemplateApplicationManager.listTemplates());
    }

    /**
     * 查询指定模板的详细信息。
     */
    @GetMapping("/{promptTemplateId}")
    public ApiResponse<PromptTemplateResponse> getTemplateDetail(@PathVariable Long promptTemplateId) {
        return ApiResponse.success(promptTemplateApplicationManager.getTemplateDetail(promptTemplateId));
    }

    /**
     * 创建新的提示词模板。
     */
    @PostMapping
    public ApiResponse<PromptTemplateResponse> createTemplate(@RequestBody PromptTemplateCreateRequest request) {
        return ApiResponse.success(promptTemplateApplicationManager.createTemplate(request));
    }

    /**
     * 更新指定模板。
     */
    @PutMapping("/{promptTemplateId}")
    public ApiResponse<PromptTemplateResponse> updateTemplate(
            @PathVariable Long promptTemplateId,
            @RequestBody PromptTemplateUpdateRequest request
    ) {
        return ApiResponse.success(promptTemplateApplicationManager.updateTemplate(promptTemplateId, request));
    }

    /**
     * 删除指定模板。
     */
    @DeleteMapping("/{promptTemplateId}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long promptTemplateId) {
        promptTemplateApplicationManager.deleteTemplate(promptTemplateId);
        return ApiResponse.success(null);
    }

    /**
     * 对指定模板执行试渲染。
     */
    @PostMapping("/{promptTemplateId}/render")
    public ApiResponse<PromptTemplateRenderResponse> renderTemplate(
            @PathVariable Long promptTemplateId,
            @RequestBody(required = false) PromptTemplateRenderRequest request
    ) {
        return ApiResponse.success(promptTemplateApplicationManager.renderTemplate(promptTemplateId, request));
    }

    /**
     * 获取模板统计信息。
     */
    @PostMapping("/statistics")
    public ApiResponse<PromptTemplateStatisticsResponse> statistics() {
        return ApiResponse.success(promptTemplateApplicationManager.statistics());
    }
}
