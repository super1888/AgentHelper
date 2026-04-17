package com.spring.ai.skills.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.skills.application.manager.SkillApplicationManager;
import com.spring.ai.skills.domain.request.SkillBatchActionRequest;
import com.spring.ai.skills.domain.request.SkillImportRequest;
import com.spring.ai.skills.domain.request.SkillSaveRequest;
import com.spring.ai.skills.domain.response.SkillExportResponse;
import com.spring.ai.skills.domain.response.SkillResponse;
import com.spring.ai.skills.domain.response.SkillStatisticsResponse;
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
@RequestMapping("/skills")
public class SkillManagementController {

    @Resource
    private SkillApplicationManager skillApplicationManager;

    @GetMapping
    public ApiResponse<List<SkillResponse>> listSkills() {
        return ApiResponse.success(skillApplicationManager.listSkills());
    }

    @GetMapping("/{skillId}")
    public ApiResponse<SkillResponse> getSkillDetail(@PathVariable Long skillId) {
        return ApiResponse.success(skillApplicationManager.getSkillDetail(skillId));
    }

    @PostMapping
    public ApiResponse<SkillResponse> createSkill(@RequestBody SkillSaveRequest request) {
        return ApiResponse.success(skillApplicationManager.createSkill(request));
    }

    @PutMapping("/{skillId}")
    public ApiResponse<SkillResponse> updateSkill(@PathVariable Long skillId, @RequestBody SkillSaveRequest request) {
        return ApiResponse.success(skillApplicationManager.updateSkill(skillId, request));
    }

    @DeleteMapping("/{skillId}")
    public ApiResponse<Void> deleteSkill(@PathVariable Long skillId) {
        skillApplicationManager.deleteSkill(skillId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{skillId}/publish")
    public ApiResponse<SkillResponse> publishSkill(@PathVariable Long skillId) {
        return ApiResponse.success(skillApplicationManager.publishSkill(skillId));
    }

    @PostMapping("/{skillId}/hot-update")
    public ApiResponse<SkillResponse> hotUpdateSkill(@PathVariable Long skillId) {
        return ApiResponse.success(skillApplicationManager.hotUpdateSkill(skillId));
    }

    @PostMapping("/statistics")
    public ApiResponse<SkillStatisticsResponse> statistics() {
        return ApiResponse.success(skillApplicationManager.statistics());
    }

    @PostMapping("/batch/delete")
    public ApiResponse<Void> batchDelete(@RequestBody SkillBatchActionRequest request) {
        skillApplicationManager.batchDelete(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/batch/status")
    public ApiResponse<List<SkillResponse>> batchUpdateStatus(@RequestBody SkillBatchActionRequest request) {
        return ApiResponse.success(skillApplicationManager.batchUpdateStatus(request));
    }

    @PostMapping("/import")
    public ApiResponse<SkillResponse> importSkill(@RequestBody SkillImportRequest request) {
        return ApiResponse.success(skillApplicationManager.importSkill(request));
    }

    @GetMapping("/{skillId}/export")
    public ApiResponse<SkillExportResponse> exportSkill(@PathVariable Long skillId) {
        return ApiResponse.success(skillApplicationManager.exportSkill(skillId));
    }
}
