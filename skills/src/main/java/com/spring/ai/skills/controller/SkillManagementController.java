package com.spring.ai.skills.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.skills.application.manager.SkillApplicationManager;
import com.spring.ai.skills.domain.request.SkillBatchActionRequest;
import com.spring.ai.skills.domain.request.SkillCopyRequest;
import com.spring.ai.skills.domain.request.SkillDebugRequest;
import com.spring.ai.skills.domain.request.SkillImportRequest;
import com.spring.ai.skills.domain.request.SkillLogQueryRequest;
import com.spring.ai.skills.domain.request.SkillSaveRequest;
import com.spring.ai.skills.domain.request.SkillTestCaseSaveRequest;
import com.spring.ai.skills.domain.request.SkillVersionCompareRequest;
import com.spring.ai.skills.domain.request.SkillVersionRollbackRequest;
import com.spring.ai.skills.domain.response.SkillDebugResponse;
import com.spring.ai.skills.domain.response.SkillExecutionLogResponse;
import com.spring.ai.skills.domain.response.SkillExportResponse;
import com.spring.ai.skills.domain.response.SkillResponse;
import com.spring.ai.skills.domain.response.SkillStatisticsResponse;
import com.spring.ai.skills.domain.response.SkillTestCaseResponse;
import com.spring.ai.skills.domain.response.SkillVersionCompareResponse;
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
 * 文件用途：Skills 管理模块对外接口
 * 核心功能：提供技能配置、版本、调试、测试、日志、批量操作与导入导出等后端接口
 */
@RestController
@RequestMapping("/skills")
public class SkillManagementController {

    @Resource
    private SkillApplicationManager skillApplicationManager;

    /**
     * 查询技能列表。
     */
    @GetMapping
    public ApiResponse<List<SkillResponse>> listSkills() {
        return ApiResponse.success(skillApplicationManager.listSkills());
    }

    /**
     * 查询已删除技能列表。
     */
    @GetMapping("/deleted")
    public ApiResponse<List<SkillResponse>> listDeletedSkills() {
        return ApiResponse.success(skillApplicationManager.listDeletedSkills());
    }

    /**
     * 查询技能详情。
     */
    @GetMapping("/{skillId}")
    public ApiResponse<SkillResponse> getSkillDetail(@PathVariable Long skillId) {
        return ApiResponse.success(skillApplicationManager.getSkillDetail(skillId));
    }

    /**
     * 创建技能。
     */
    @PostMapping
    public ApiResponse<SkillResponse> createSkill(@RequestBody SkillSaveRequest request) {
        return ApiResponse.success(skillApplicationManager.createSkill(request));
    }

    /**
     * 更新技能。
     */
    @PutMapping("/{skillId}")
    public ApiResponse<SkillResponse> updateSkill(@PathVariable Long skillId, @RequestBody SkillSaveRequest request) {
        return ApiResponse.success(skillApplicationManager.updateSkill(skillId, request));
    }

    /**
     * 删除技能。
     */
    @DeleteMapping("/{skillId}")
    public ApiResponse<Void> deleteSkill(@PathVariable Long skillId) {
        skillApplicationManager.deleteSkill(skillId);
        return ApiResponse.success(null);
    }

    /**
     * 恢复已删除技能。
     */
    @PostMapping("/{skillId}/restore")
    public ApiResponse<SkillResponse> restoreSkill(@PathVariable Long skillId) {
        return ApiResponse.success(skillApplicationManager.restoreSkill(skillId));
    }

    /**
     * 发布技能。
     */
    @PostMapping("/{skillId}/publish")
    public ApiResponse<SkillResponse> publishSkill(@PathVariable Long skillId) {
        return ApiResponse.success(skillApplicationManager.publishSkill(skillId));
    }

    /**
     * 下线技能。
     */
    @PostMapping("/{skillId}/offline")
    public ApiResponse<SkillResponse> offlineSkill(@PathVariable Long skillId) {
        return ApiResponse.success(skillApplicationManager.offlineSkill(skillId));
    }

    /**
     * 热更新发布技能。
     */
    @PostMapping("/{skillId}/hot-update")
    public ApiResponse<SkillResponse> hotUpdateSkill(@PathVariable Long skillId) {
        return ApiResponse.success(skillApplicationManager.hotUpdateSkill(skillId));
    }

    /**
     * 回滚技能版本。
     */
    @PostMapping("/{skillId}/rollback")
    public ApiResponse<SkillResponse> rollbackSkill(
            @PathVariable Long skillId,
            @RequestBody SkillVersionRollbackRequest request
    ) {
        return ApiResponse.success(skillApplicationManager.rollbackSkill(skillId, request));
    }

    /**
     * 对比技能版本。
     */
    @PostMapping("/{skillId}/compare")
    public ApiResponse<SkillVersionCompareResponse> compareVersions(
            @PathVariable Long skillId,
            @RequestBody SkillVersionCompareRequest request
    ) {
        return ApiResponse.success(skillApplicationManager.compareVersions(skillId, request));
    }

    /**
     * 复制技能。
     */
    @PostMapping("/{skillId}/copy")
    public ApiResponse<SkillResponse> copySkill(@PathVariable Long skillId, @RequestBody SkillCopyRequest request) {
        return ApiResponse.success(skillApplicationManager.copySkill(skillId, request));
    }

    /**
     * 导出技能。
     */
    @GetMapping("/{skillId}/export")
    public ApiResponse<SkillExportResponse> exportSkill(@PathVariable Long skillId) {
        return ApiResponse.success(skillApplicationManager.exportSkill(skillId));
    }

    /**
     * 导入技能。
     */
    @PostMapping("/import")
    public ApiResponse<SkillResponse> importSkill(@RequestBody SkillImportRequest request) {
        return ApiResponse.success(skillApplicationManager.importSkill(request));
    }

    /**
     * 查询统计信息。
     */
    @PostMapping("/statistics")
    public ApiResponse<SkillStatisticsResponse> statistics() {
        return ApiResponse.success(skillApplicationManager.statistics());
    }

    /**
     * 批量删除技能。
     */
    @PostMapping("/batch/delete")
    public ApiResponse<Void> batchDelete(@RequestBody SkillBatchActionRequest request) {
        skillApplicationManager.batchDelete(request);
        return ApiResponse.success(null);
    }

    /**
     * 批量修改技能状态。
     */
    @PostMapping("/batch/status")
    public ApiResponse<List<SkillResponse>> batchUpdateStatus(@RequestBody SkillBatchActionRequest request) {
        return ApiResponse.success(skillApplicationManager.batchUpdateStatus(request));
    }

    /**
     * 批量更新技能标签。
     */
    @PostMapping("/batch/tags")
    public ApiResponse<List<SkillResponse>> batchUpdateTags(@RequestBody SkillBatchActionRequest request) {
        return ApiResponse.success(skillApplicationManager.batchUpdateTags(request));
    }

    /**
     * 批量迁移技能分类。
     */
    @PostMapping("/batch/category")
    public ApiResponse<List<SkillResponse>> batchMoveCategory(@RequestBody SkillBatchActionRequest request) {
        return ApiResponse.success(skillApplicationManager.batchMoveCategory(request));
    }

    /**
     * 批量发布技能。
     */
    @PostMapping("/batch/publish")
    public ApiResponse<List<SkillResponse>> batchPublish(@RequestBody SkillBatchActionRequest request) {
        return ApiResponse.success(skillApplicationManager.batchPublish(request));
    }

    /**
     * 批量下线技能。
     */
    @PostMapping("/batch/offline")
    public ApiResponse<List<SkillResponse>> batchOffline(@RequestBody SkillBatchActionRequest request) {
        return ApiResponse.success(skillApplicationManager.batchOffline(request));
    }

    /**
     * 查询技能测试用例。
     */
    @GetMapping("/{skillId}/test-cases")
    public ApiResponse<List<SkillTestCaseResponse>> listTestCases(@PathVariable Long skillId) {
        return ApiResponse.success(skillApplicationManager.listTestCases(skillId));
    }

    /**
     * 创建技能测试用例。
     */
    @PostMapping("/{skillId}/test-cases")
    public ApiResponse<SkillTestCaseResponse> createTestCase(
            @PathVariable Long skillId,
            @RequestBody SkillTestCaseSaveRequest request
    ) {
        return ApiResponse.success(skillApplicationManager.createTestCase(skillId, request));
    }

    /**
     * 更新技能测试用例。
     */
    @PutMapping("/test-cases/{testCaseId}")
    public ApiResponse<SkillTestCaseResponse> updateTestCase(
            @PathVariable Long testCaseId,
            @RequestBody SkillTestCaseSaveRequest request
    ) {
        return ApiResponse.success(skillApplicationManager.updateTestCase(testCaseId, request));
    }

    /**
     * 删除技能测试用例。
     */
    @DeleteMapping("/test-cases/{testCaseId}")
    public ApiResponse<Void> deleteTestCase(@PathVariable Long testCaseId) {
        skillApplicationManager.deleteTestCase(testCaseId);
        return ApiResponse.success(null);
    }

    /**
     * 运行技能测试用例。
     */
    @PostMapping("/test-cases/{testCaseId}/run")
    public ApiResponse<SkillDebugResponse> runTestCase(@PathVariable Long testCaseId) {
        return ApiResponse.success(skillApplicationManager.runTestCase(testCaseId));
    }

    /**
     * 在线调试技能。
     */
    @PostMapping("/debug")
    public ApiResponse<SkillDebugResponse> debugSkill(@RequestBody SkillDebugRequest request) {
        return ApiResponse.success(skillApplicationManager.debugSkill(request));
    }

    /**
     * 查询技能执行日志。
     */
    @PostMapping("/logs/query")
    public ApiResponse<List<SkillExecutionLogResponse>> listLogs(@RequestBody SkillLogQueryRequest request) {
        return ApiResponse.success(skillApplicationManager.listLogs(request));
    }
}
