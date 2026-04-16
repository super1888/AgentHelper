package com.spring.ai.prompt.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.prompt.application.manager.PromptTemplateApplicationManager;
import com.spring.ai.prompt.domain.request.PromptTemplateCreateRequest;
import com.spring.ai.prompt.domain.request.PromptTemplateUpdateRequest;
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

@RestController
@RequestMapping("/promptTemplates")
public class PromptTemplateController {

    @Resource
    private PromptTemplateApplicationManager promptTemplateApplicationManager;

    @GetMapping
    public ApiResponse<List<PromptTemplateResponse>> listTemplates() {
        return ApiResponse.success(promptTemplateApplicationManager.listTemplates());
    }

    @GetMapping("/{promptTemplateId}")
    public ApiResponse<PromptTemplateResponse> getTemplateDetail(@PathVariable Long promptTemplateId) {
        return ApiResponse.success(promptTemplateApplicationManager.getTemplateDetail(promptTemplateId));
    }

    @PostMapping
    public ApiResponse<PromptTemplateResponse> createTemplate(@RequestBody PromptTemplateCreateRequest request) {
        return ApiResponse.success(promptTemplateApplicationManager.createTemplate(request));
    }

    @PutMapping("/{promptTemplateId}")
    public ApiResponse<PromptTemplateResponse> updateTemplate(
            @PathVariable Long promptTemplateId,
            @RequestBody PromptTemplateUpdateRequest request
    ) {
        return ApiResponse.success(promptTemplateApplicationManager.updateTemplate(promptTemplateId, request));
    }

    @DeleteMapping("/{promptTemplateId}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long promptTemplateId) {
        promptTemplateApplicationManager.deleteTemplate(promptTemplateId);
        return ApiResponse.success(null);
    }

    @PostMapping("/statistics")
    public ApiResponse<PromptTemplateStatisticsResponse> statistics() {
        return ApiResponse.success(promptTemplateApplicationManager.statistics());
    }
}
