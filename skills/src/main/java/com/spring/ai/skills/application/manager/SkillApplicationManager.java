package com.spring.ai.skills.application.manager;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.SkillRecord;
import com.spring.ai.common.repository.enitiy.SkillVersionRecord;
import com.spring.ai.common.repository.service.SkillRecordService;
import com.spring.ai.common.repository.service.SkillVersionRecordService;
import com.spring.ai.skills.application.assmbler.SkillAssembler;
import com.spring.ai.common.constants.SkillManagementConstants;
import com.spring.ai.skills.domain.dto.SkillSnapshotDTO;
import com.spring.ai.skills.domain.request.SkillBatchActionRequest;
import com.spring.ai.skills.domain.request.SkillImportRequest;
import com.spring.ai.skills.domain.request.SkillSaveRequest;
import com.spring.ai.skills.domain.response.SkillExportResponse;
import com.spring.ai.skills.domain.response.SkillResponse;
import com.spring.ai.skills.domain.response.SkillStatisticsResponse;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文件用途：Skill 管理应用层编排入口
 * 作者：Codex
 * 创建时间：2026-04-17
 * 核心功能：负责用户管理 Skill 的增删改查、版本流转、发布热更新、批量操作与导入导出
 */
@Component
public class SkillApplicationManager {

    private static final String DEFAULT_SKILL_CATEGORY = "USER_MANAGEMENT";

    @Resource
    private SkillRecordService skillRecordService;

    @Resource
    private SkillVersionRecordService skillVersionRecordService;

    @Resource
    private SkillSupportManager skillSupportManager;

    /**
     * 查询当前租户下的 Skill 列表，并组装版本与配置快照。
     */
    public List<SkillResponse> listSkills() {
        Long tenantId = skillSupportManager.getCurrentTenantId();
        return skillRecordService.listByTenantId(tenantId).stream()
                .map(record -> SkillAssembler.toResponse(
                        record,
                        skillSupportManager.parseSnapshot(record.getExt()),
                        skillVersionRecordService.listBySkillId(record.getId(), tenantId)))
                .toList();
    }

    /**
     * 查询单个 Skill 详情。
     */
    public SkillResponse getSkillDetail(Long skillId) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        return SkillAssembler.toResponse(
                record,
                skillSupportManager.parseSnapshot(record.getExt()),
                skillVersionRecordService.listBySkillId(record.getId(), record.getTenantId()));
    }

    /**
     * 创建 Skill，并写入首个版本快照。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse createSkill(SkillSaveRequest request) {
        validateSaveRequest(request, true);
        Long tenantId = skillSupportManager.getCurrentTenantId();
        if (skillRecordService.getBySkillCode(tenantId, request.getSkillCode()) != null) {
            throw new BusinessException(
                    ErrorCodeEnum.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "Skill 编码已存在");
        }
        SkillRecord record = new SkillRecord();
        record.setSkillCode(request.getSkillCode().trim());
        record.setSkillName(request.getSkillName().trim());
        record.setDescription(trimToNull(request.getDescription()));
        record.setSkillCategory(defaultText(request.getSkillCategory(), DEFAULT_SKILL_CATEGORY));
        record.setSkillStatus(defaultText(request.getSkillStatus(), SkillManagementConstants.SKILL_STATUS_ENABLED));
        record.setPublishStatus(SkillManagementConstants.PUBLISH_STATUS_DRAFT);
        record.setVersionMode(defaultText(request.getVersionMode(), SkillManagementConstants.VERSION_MODE_MANUAL));
        record.setCurrentVersionNo(1);
        record.setLatestVersionNo(1);
        record.setPublishedVersionNo(null);
        record.setHotUpdateEnabled(defaultFlag(request.getHotUpdateEnabled()));
        record.setTenantId(tenantId);
        record.setOwnerUserId(skillSupportManager.getCurrentUserId());
        record.setOwnerUserName(skillSupportManager.getCurrentUserName());
        record.setRemark(trimToNull(request.getRemark()));
        record.setExt(skillSupportManager.toJson(toSnapshot(request, SkillManagementConstants.PUBLISH_STATUS_DRAFT)));
        skillRecordService.save(record);
        // 首次创建时同步生成当前版本记录，确保发布链路和版本追踪可直接使用。
        createVersion(record, 1, SkillManagementConstants.VERSION_STATUS_CURRENT, SkillManagementConstants.PUBLISH_STATUS_DRAFT);
        return getSkillDetail(record.getId());
    }

    /**
     * 更新 Skill，并把当前版本推进到新版本号。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse updateSkill(Long skillId, SkillSaveRequest request) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        validateSaveRequest(request, false);
        record.setSkillName(request.getSkillName().trim());
        record.setDescription(trimToNull(request.getDescription()));
        record.setSkillCategory(defaultText(request.getSkillCategory(), record.getSkillCategory()));
        record.setSkillStatus(defaultText(request.getSkillStatus(), record.getSkillStatus()));
        record.setVersionMode(defaultText(request.getVersionMode(), record.getVersionMode()));
        record.setHotUpdateEnabled(defaultFlag(request.getHotUpdateEnabled()));
        record.setRemark(trimToNull(request.getRemark()));
        int nextVersionNo = record.getLatestVersionNo() == null ? 1 : record.getLatestVersionNo() + 1;
        record.setCurrentVersionNo(nextVersionNo);
        record.setLatestVersionNo(nextVersionNo);
        record.setExt(skillSupportManager.toJson(toSnapshot(request, record.getPublishStatus())));
        skillRecordService.updateById(record);
        // 只保留一个当前版本，其余旧版本统一转为历史版本。
        skillVersionRecordService.listBySkillId(record.getId(), record.getTenantId()).forEach(item -> {
            if (SkillManagementConstants.VERSION_STATUS_CURRENT.equals(item.getVersionStatus())) {
                item.setVersionStatus(SkillManagementConstants.VERSION_STATUS_HISTORY);
                skillVersionRecordService.updateById(item);
            }
        });
        createVersion(record, nextVersionNo, SkillManagementConstants.VERSION_STATUS_CURRENT, record.getPublishStatus());
        return getSkillDetail(record.getId());
    }

    /**
     * 删除 Skill，同时清理其版本记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSkill(Long skillId) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        List<Long> versionIds = skillVersionRecordService.listBySkillId(record.getId(), record.getTenantId()).stream()
                .map(SkillVersionRecord::getId)
                .toList();
        if (!versionIds.isEmpty()) {
            skillVersionRecordService.removeByIds(versionIds);
        }
        skillRecordService.removeById(record.getId());
    }

    /**
     * 发布当前 Skill 版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse publishSkill(Long skillId) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        record.setPublishStatus(SkillManagementConstants.PUBLISH_STATUS_PUBLISHED);
        record.setPublishedVersionNo(record.getCurrentVersionNo());
        SkillSnapshotDTO snapshot = skillSupportManager.parseSnapshot(record.getExt());
        snapshot.setPublishStatus(SkillManagementConstants.PUBLISH_STATUS_PUBLISHED);
        record.setExt(skillSupportManager.toJson(snapshot));
        skillRecordService.updateById(record);
        SkillVersionRecord versionRecord = skillVersionRecordService.getBySkillIdAndVersionNo(
                record.getId(), record.getTenantId(), record.getCurrentVersionNo());
        if (versionRecord != null) {
            versionRecord.setPublishStatus(SkillManagementConstants.PUBLISH_STATUS_PUBLISHED);
            skillVersionRecordService.updateById(versionRecord);
        }
        return getSkillDetail(record.getId());
    }

    /**
     * 开启 Skill 热更新。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse hotUpdateSkill(Long skillId) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        record.setHotUpdateEnabled(1);
        SkillSnapshotDTO snapshot = skillSupportManager.parseSnapshot(record.getExt());
        snapshot.setHotUpdateEnabled(1);
        record.setExt(skillSupportManager.toJson(snapshot));
        skillRecordService.updateById(record);
        return getSkillDetail(record.getId());
    }

    /**
     * 批量删除 Skill。
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(SkillBatchActionRequest request) {
        validateBatchRequest(request);
        request.getSkillIds().forEach(this::deleteSkill);
    }

    /**
     * 批量更新 Skill 状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SkillResponse> batchUpdateStatus(SkillBatchActionRequest request) {
        validateBatchRequest(request);
        if (!StringUtils.hasText(request.getSkillStatus())) {
            throw new BusinessException(
                    ErrorCodeEnum.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "Skill 状态不能为空");
        }
        request.getSkillIds().forEach(skillId -> {
            SkillRecord record = skillSupportManager.requireSkill(skillId);
            record.setSkillStatus(request.getSkillStatus().trim());
            skillRecordService.updateById(record);
        });
        return listSkills();
    }

    /**
     * 统计 Skill 关键指标。
     */
    public SkillStatisticsResponse statistics() {
        List<SkillRecord> records = skillRecordService.listByTenantId(skillSupportManager.getCurrentTenantId());
        return SkillStatisticsResponse.builder()
                .totalCount(records.size())
                .enabledCount((int) records.stream()
                        .filter(item -> SkillManagementConstants.SKILL_STATUS_ENABLED.equals(item.getSkillStatus()))
                        .count())
                .publishedCount((int) records.stream()
                        .filter(item -> SkillManagementConstants.PUBLISH_STATUS_PUBLISHED.equals(item.getPublishStatus()))
                        .count())
                .hotUpdateEnabledCount((int) records.stream()
                        .filter(item -> Integer.valueOf(1).equals(item.getHotUpdateEnabled()))
                        .count())
                .build();
    }

    /**
     * 导出 Skill 快照。
     */
    public SkillExportResponse exportSkill(Long skillId) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        return SkillExportResponse.builder()
                .skillCode(record.getSkillCode())
                .skillName(record.getSkillName())
                .exportPayload(record.getExt())
                .build();
    }

    /**
     * 导入 Skill 快照，并复用创建链路落库。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse importSkill(SkillImportRequest request) {
        if (request == null || !StringUtils.hasText(request.getImportPayload())) {
            throw new BusinessException(
                    ErrorCodeEnum.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "导入内容不能为空");
        }
        SkillSnapshotDTO snapshot = skillSupportManager.parseSnapshot(request.getImportPayload());
        SkillSaveRequest saveRequest = new SkillSaveRequest();
        saveRequest.setSkillCode(snapshot.getSkillCode());
        saveRequest.setSkillName(snapshot.getSkillName());
        saveRequest.setDescription(snapshot.getDescription());
        saveRequest.setSkillCategory(snapshot.getSkillCategory());
        saveRequest.setSkillStatus(snapshot.getSkillStatus());
        saveRequest.setVersionMode(snapshot.getVersionMode());
        saveRequest.setHotUpdateEnabled(snapshot.getHotUpdateEnabled());
        saveRequest.setIntentConfigs(snapshot.getIntentConfigs());
        saveRequest.setExecutionConfig(snapshot.getExecutionConfig());
        saveRequest.setRoutingConfig(snapshot.getRoutingConfig());
        saveRequest.setPermissionConfig(snapshot.getPermissionConfig());
        saveRequest.setObservabilityConfig(snapshot.getObservabilityConfig());
        saveRequest.setReleaseConfig(snapshot.getReleaseConfig());
        saveRequest.setBatchConfig(snapshot.getBatchConfig());
        saveRequest.setWorkflowConfig(snapshot.getWorkflowConfig());
        saveRequest.setRemark(snapshot.getRemark());
        return createSkill(saveRequest);
    }

    /**
     * 校验保存参数是否满足最小要求。
     */
    private void validateSaveRequest(SkillSaveRequest request, boolean createMode) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "Skill 请求参数不能为空");
        }
        if (createMode && !StringUtils.hasText(request.getSkillCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "Skill 编码不能为空");
        }
        if (!StringUtils.hasText(request.getSkillName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "Skill 名称不能为空");
        }
        if (request.getExecutionConfig() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "API / 函数执行配置不能为空");
        }
        if (request.getRoutingConfig() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "路由调度与上下文配置不能为空");
        }
        if (request.getPermissionConfig() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "权限与风控配置不能为空");
        }
    }

    /**
     * 校验批量操作参数。
     */
    private void validateBatchRequest(SkillBatchActionRequest request) {
        if (request == null || request.getSkillIds() == null || request.getSkillIds().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "批量操作的 Skill 不能为空");
        }
    }

    /**
     * 把请求对象收敛为可持久化的 Skill 配置快照。
     */
    private SkillSnapshotDTO toSnapshot(SkillSaveRequest request, String publishStatus) {
        return SkillSnapshotDTO.builder()
                .skillCode(trimToNull(request.getSkillCode()))
                .skillName(trimToNull(request.getSkillName()))
                .description(trimToNull(request.getDescription()))
                .skillCategory(defaultText(request.getSkillCategory(), DEFAULT_SKILL_CATEGORY))
                .skillStatus(defaultText(request.getSkillStatus(), SkillManagementConstants.SKILL_STATUS_ENABLED))
                .publishStatus(defaultText(publishStatus, SkillManagementConstants.PUBLISH_STATUS_DRAFT))
                .versionMode(defaultText(request.getVersionMode(), SkillManagementConstants.VERSION_MODE_MANUAL))
                .hotUpdateEnabled(defaultFlag(request.getHotUpdateEnabled()))
                .intentConfigs(request.getIntentConfigs() == null ? List.of() : request.getIntentConfigs())
                .executionConfig(request.getExecutionConfig())
                .routingConfig(request.getRoutingConfig())
                .permissionConfig(request.getPermissionConfig())
                .observabilityConfig(request.getObservabilityConfig())
                .releaseConfig(request.getReleaseConfig())
                .batchConfig(request.getBatchConfig())
                .workflowConfig(request.getWorkflowConfig())
                .remark(trimToNull(request.getRemark()))
                .build();
    }

    /**
     * 写入版本快照记录。
     */
    private void createVersion(SkillRecord record, Integer versionNo, String versionStatus, String publishStatus) {
        SkillVersionRecord versionRecord = new SkillVersionRecord();
        versionRecord.setSkillId(record.getId());
        versionRecord.setSkillCode(record.getSkillCode());
        versionRecord.setSkillName(record.getSkillName());
        versionRecord.setTenantId(record.getTenantId());
        versionRecord.setVersionNo(versionNo);
        versionRecord.setVersionStatus(versionStatus);
        versionRecord.setPublishStatus(publishStatus);
        versionRecord.setSnapshotJson(record.getExt());
        skillVersionRecordService.save(versionRecord);
    }

    /**
     * 字符串为空时转为 null，便于数据库统一处理。
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 返回去空白后的值，若为空则回退默认值。
     */
    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    /**
     * 统一将前端开关值收敛为 0/1。
     */
    private Integer defaultFlag(Integer value) {
        return Integer.valueOf(1).equals(value) ? 1 : 0;
    }
}
