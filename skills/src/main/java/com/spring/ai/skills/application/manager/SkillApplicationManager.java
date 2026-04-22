package com.spring.ai.skills.application.manager;

import com.spring.ai.common.exception.BusinessExceptions;
import com.spring.ai.common.repository.enitiy.SkillExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.SkillRecord;
import com.spring.ai.common.repository.enitiy.SkillTestCaseRecord;
import com.spring.ai.common.repository.enitiy.SkillVersionRecord;
import com.spring.ai.common.repository.service.SkillExecutionLogRecordService;
import com.spring.ai.common.repository.service.SkillRecordService;
import com.spring.ai.common.repository.service.SkillTestCaseRecordService;
import com.spring.ai.common.repository.service.SkillVersionRecordService;
import com.spring.ai.common.utils.CommonTextUtils;
import com.spring.ai.skills.application.assmbler.SkillAssembler;
import com.spring.ai.skills.config.SkillManagementConstants;
import com.spring.ai.skills.domain.dto.SkillCategoryDTO;
import com.spring.ai.skills.domain.dto.SkillDebugTraceStepDTO;
import com.spring.ai.skills.domain.dto.SkillSnapshotDTO;
import com.spring.ai.skills.domain.dto.SkillTagDTO;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文件用途：Skills 管理应用服务
 * 核心功能：封装技能配置、版本、发布、回收、批量、导入导出、调试、测试用例和日志业务逻辑
 */
@Component
public class SkillApplicationManager {

    private static final String DEFAULT_SKILL_TYPE = "API_CALL";
    private static final String DEFAULT_SKILL_CATEGORY = "USER_MANAGEMENT";
    private static final String DEFAULT_CHANNEL_CODE = "WEB";
    private static final String DEFAULT_LOCALE = "zh-CN";

    @Resource
    private SkillRecordService skillRecordService;

    @Resource
    private SkillVersionRecordService skillVersionRecordService;

    @Resource
    private SkillTestCaseRecordService skillTestCaseRecordService;

    @Resource
    private SkillExecutionLogRecordService skillExecutionLogRecordService;

    @Resource
    private SkillSupportManager skillSupportManager;

    /**
     * 查询当前租户未删除的技能列表。
     */
    public List<SkillResponse> listSkills() {
        Long tenantId = skillSupportManager.getCurrentTenantId();
        return skillRecordService.listByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 查询当前租户回收站中的技能列表。
     */
    public List<SkillResponse> listDeletedSkills() {
        Long tenantId = skillSupportManager.getCurrentTenantId();
        return skillRecordService.listDeletedByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 查询技能详情。
     */
    public SkillResponse getSkillDetail(Long skillId) {
        return toResponse(skillSupportManager.requireSkill(skillId));
    }

    /**
     * 创建技能并生成初始版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse createSkill(SkillSaveRequest request) {
        validateSaveRequest(request, true);
        Long tenantId = skillSupportManager.getCurrentTenantId();
        String skillCode = request.getSkillCode().trim();
        if (skillRecordService.getBySkillCode(tenantId, skillCode) != null) {
            throw BusinessExceptions.badRequest("技能编码已存在：" + skillCode);
        }

        SkillRecord record = new SkillRecord();
        record.setSkillCode(skillCode);
        fillRecord(record, request, SkillManagementConstants.PUBLISH_STATUS_DRAFT);
        record.setCurrentVersionNo(1);
        record.setLatestVersionNo(1);
        record.setPublishedVersionNo(null);
        record.setDeletedFlag(0);
        record.setTenantId(tenantId);
        record.setOwnerUserId(skillSupportManager.getCurrentUserId());
        record.setOwnerUserName(skillSupportManager.getCurrentUserName());
        record.setExt(skillSupportManager.toJson(toSnapshot(request, SkillManagementConstants.PUBLISH_STATUS_DRAFT)));
        skillRecordService.save(record);
        createVersion(record, 1, SkillManagementConstants.VERSION_STATUS_CURRENT, SkillManagementConstants.PUBLISH_STATUS_DRAFT);
        return getSkillDetail(record.getId());
    }

    /**
     * 更新技能并创建新的当前版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse updateSkill(Long skillId, SkillSaveRequest request) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        validateSaveRequest(request, false);
        int nextVersionNo = record.getLatestVersionNo() == null ? 1 : record.getLatestVersionNo() + 1;
        markCurrentVersionAsHistory(record);
        fillRecord(record, request, record.getPublishStatus());
        record.setCurrentVersionNo(nextVersionNo);
        record.setLatestVersionNo(nextVersionNo);
        record.setExt(skillSupportManager.toJson(toSnapshot(request, record.getPublishStatus())));
        skillRecordService.updateById(record);
        createVersion(record, nextVersionNo, SkillManagementConstants.VERSION_STATUS_CURRENT, record.getPublishStatus());
        return getSkillDetail(record.getId());
    }

    /**
     * 将技能移入回收站，保留版本、测试用例和日志以支持恢复。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSkill(Long skillId) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        record.setDeletedFlag(1);
        skillRecordService.updateById(record);
    }

    /**
     * 从回收站恢复技能。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse restoreSkill(Long skillId) {
        SkillRecord record = requireDeletedSkill(skillId);
        record.setDeletedFlag(0);
        skillRecordService.updateById(record);
        return getSkillDetail(record.getId());
    }

    /**
     * 发布当前版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse publishSkill(Long skillId) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        record.setPublishStatus(SkillManagementConstants.PUBLISH_STATUS_PUBLISHED);
        record.setPublishedVersionNo(record.getCurrentVersionNo());
        updateSnapshotPublishStatus(record, SkillManagementConstants.PUBLISH_STATUS_PUBLISHED);
        updateCurrentVersionPublishStatus(record, SkillManagementConstants.PUBLISH_STATUS_PUBLISHED);
        return getSkillDetail(record.getId());
    }

    /**
     * 下线已发布技能。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse offlineSkill(Long skillId) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        record.setPublishStatus(SkillManagementConstants.PUBLISH_STATUS_OFFLINE);
        updateSnapshotPublishStatus(record, SkillManagementConstants.PUBLISH_STATUS_OFFLINE);
        updateCurrentVersionPublishStatus(record, SkillManagementConstants.PUBLISH_STATUS_OFFLINE);
        return getSkillDetail(record.getId());
    }

    /**
     * 打开技能热更新开关。
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
     * 回滚到指定历史版本，并把回滚结果作为新的当前版本保存。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse rollbackSkill(Long skillId, SkillVersionRollbackRequest request) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        if (request == null || request.getTargetVersionNo() == null) {
            throw BusinessExceptions.badRequest("目标版本号不能为空");
        }
        SkillVersionRecord targetVersion = requireVersion(record, request.getTargetVersionNo());
        SkillSnapshotDTO snapshot = skillSupportManager.parseSnapshot(targetVersion.getSnapshotJson());
        if (StringUtils.hasText(request.getVersionDescription())) {
            snapshot.setVersionDescription(request.getVersionDescription().trim());
        }
        snapshot.setPublishStatus(record.getPublishStatus());

        int nextVersionNo = record.getLatestVersionNo() == null ? 1 : record.getLatestVersionNo() + 1;
        markCurrentVersionAsHistory(record);
        record.setSkillName(CommonTextUtils.defaultText(snapshot.getSkillName(), record.getSkillName()));
        record.setDescription(CommonTextUtils.trimToNull(snapshot.getDescription()));
        record.setSkillType(CommonTextUtils.defaultText(snapshot.getSkillType(), record.getSkillType()));
        record.setSkillCategory(CommonTextUtils.defaultText(snapshot.getSkillCategory(), record.getSkillCategory()));
        record.setSkillStatus(CommonTextUtils.defaultText(snapshot.getSkillStatus(), record.getSkillStatus()));
        record.setVersionMode(CommonTextUtils.defaultText(snapshot.getVersionMode(), record.getVersionMode()));
        record.setSortWeight(snapshot.getSortWeight() == null ? record.getSortWeight() : snapshot.getSortWeight());
        record.setHotUpdateEnabled(defaultFlag(snapshot.getHotUpdateEnabled()));
        record.setRemark(CommonTextUtils.trimToNull(snapshot.getRemark()));
        record.setCurrentVersionNo(nextVersionNo);
        record.setLatestVersionNo(nextVersionNo);
        record.setExt(skillSupportManager.toJson(snapshot));
        skillRecordService.updateById(record);
        createVersion(record, nextVersionNo, SkillManagementConstants.VERSION_STATUS_ROLLBACK, record.getPublishStatus());
        return getSkillDetail(record.getId());
    }

    /**
     * 对比两个版本快照。
     */
    public SkillVersionCompareResponse compareVersions(Long skillId, SkillVersionCompareRequest request) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        if (request == null || request.getSourceVersionNo() == null || request.getTargetVersionNo() == null) {
            throw BusinessExceptions.badRequest("源版本号和目标版本号不能为空");
        }
        SkillVersionRecord source = requireVersion(record, request.getSourceVersionNo());
        SkillVersionRecord target = requireVersion(record, request.getTargetVersionNo());
        return SkillVersionCompareResponse.builder()
                .sourceVersionNo(source.getVersionNo())
                .targetVersionNo(target.getVersionNo())
                .sourceSnapshotJson(source.getSnapshotJson())
                .targetSnapshotJson(target.getSnapshotJson())
                .diffSummary(buildDiffSummary(source.getSnapshotJson(), target.getSnapshotJson()))
                .build();
    }

    /**
     * 复制技能，可选复制测试用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse copySkill(Long skillId, SkillCopyRequest request) {
        SkillRecord source = skillSupportManager.requireSkill(skillId);
        if (request == null || !StringUtils.hasText(request.getNewSkillCode()) || !StringUtils.hasText(request.getNewSkillName())) {
            throw BusinessExceptions.badRequest("新技能编码和名称不能为空");
        }
        SkillSnapshotDTO snapshot = skillSupportManager.parseSnapshot(source.getExt());
        SkillSaveRequest saveRequest = new SkillSaveRequest();
        fillSaveRequestFromSnapshot(saveRequest, snapshot);
        saveRequest.setSkillCode(request.getNewSkillCode().trim());
        saveRequest.setSkillName(request.getNewSkillName().trim());
        saveRequest.setVersionDescription("复制自 " + source.getSkillCode());
        SkillResponse copied = createSkill(saveRequest);
        if (Integer.valueOf(1).equals(request.getIncludeTestCases())) {
            duplicateTestCases(source.getId(), copied.getId(), copied.getSkillCode());
        }
        return getSkillDetail(copied.getId());
    }

    /**
     * 导出技能快照。
     */
    public SkillExportResponse exportSkill(Long skillId) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        return SkillExportResponse.builder()
                .skillCode(record.getSkillCode())
                .skillName(record.getSkillName())
                .exportFormat("JSON")
                .exportPayload(skillSupportManager.prettyJson(skillSupportManager.parseSnapshot(record.getExt())))
                .build();
    }

    /**
     * 导入技能快照，可按请求直接发布。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillResponse importSkill(SkillImportRequest request) {
        if (request == null || !StringUtils.hasText(request.getImportPayload())) {
            throw BusinessExceptions.badRequest("导入内容不能为空");
        }
        SkillSnapshotDTO snapshot = skillSupportManager.parseSnapshot(request.getImportPayload());
        SkillSaveRequest saveRequest = new SkillSaveRequest();
        fillSaveRequestFromSnapshot(saveRequest, snapshot);
        if (!StringUtils.hasText(saveRequest.getSkillCode())) {
            saveRequest.setSkillCode("SKILL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }
        SkillResponse imported = createSkill(saveRequest);
        if (Integer.valueOf(1).equals(request.getPublishAfterImport())) {
            return publishSkill(imported.getId());
        }
        return imported;
    }

    /**
     * 查询技能统计。
     */
    public SkillStatisticsResponse statistics() {
        Long tenantId = skillSupportManager.getCurrentTenantId();
        List<SkillRecord> records = skillRecordService.listByTenantId(tenantId);
        List<SkillRecord> deletedRecords = skillRecordService.listDeletedByTenantId(tenantId);
        List<SkillExecutionLogRecord> logs = skillExecutionLogRecordService.listByTenantId(tenantId);
        int testCaseCount = records.stream()
                .mapToInt(record -> skillTestCaseRecordService.listBySkillId(record.getId(), tenantId).size())
                .sum();
        return SkillStatisticsResponse.builder()
                .totalCount(records.size())
                .enabledCount((int) records.stream().filter(item -> SkillManagementConstants.SKILL_STATUS_ENABLED.equals(item.getSkillStatus())).count())
                .publishedCount((int) records.stream().filter(item -> SkillManagementConstants.PUBLISH_STATUS_PUBLISHED.equals(item.getPublishStatus())).count())
                .hotUpdateEnabledCount((int) records.stream().filter(item -> Integer.valueOf(1).equals(item.getHotUpdateEnabled())).count())
                .draftCount((int) records.stream().filter(item -> SkillManagementConstants.PUBLISH_STATUS_DRAFT.equals(item.getPublishStatus())).count())
                .deletedCount(deletedRecords.size())
                .totalTestCaseCount(testCaseCount)
                .totalLogCount(logs.size())
                .successLogCount((int) logs.stream().filter(item -> Integer.valueOf(1).equals(item.getSuccessFlag())).count())
                .failureLogCount((int) logs.stream().filter(item -> Integer.valueOf(0).equals(item.getSuccessFlag())).count())
                .build();
    }

    /**
     * 批量删除技能。
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(SkillBatchActionRequest request) {
        validateBatchRequest(request);
        request.getSkillIds().forEach(this::deleteSkill);
    }

    /**
     * 批量修改技能状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SkillResponse> batchUpdateStatus(SkillBatchActionRequest request) {
        validateBatchRequest(request);
        if (!StringUtils.hasText(request.getSkillStatus())) {
            throw BusinessExceptions.badRequest("目标技能状态不能为空");
        }
        request.getSkillIds().forEach(skillId -> {
            SkillRecord record = skillSupportManager.requireSkill(skillId);
            record.setSkillStatus(request.getSkillStatus().trim());
            SkillSnapshotDTO snapshot = skillSupportManager.parseSnapshot(record.getExt());
            snapshot.setSkillStatus(record.getSkillStatus());
            record.setExt(skillSupportManager.toJson(snapshot));
            skillRecordService.updateById(record);
        });
        return listSkills();
    }

    /**
     * 批量覆盖技能标签。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SkillResponse> batchUpdateTags(SkillBatchActionRequest request) {
        validateBatchRequest(request);
        List<SkillTagDTO> tags = request.getTagNames() == null ? List.of() : request.getTagNames().stream()
                .filter(StringUtils::hasText)
                .map(this::toTag)
                .toList();
        request.getSkillIds().forEach(skillId -> {
            SkillRecord record = skillSupportManager.requireSkill(skillId);
            SkillSnapshotDTO snapshot = skillSupportManager.parseSnapshot(record.getExt());
            snapshot.setTags(tags);
            record.setExt(skillSupportManager.toJson(snapshot));
            skillRecordService.updateById(record);
        });
        return listSkills();
    }

    /**
     * 批量迁移技能分类。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SkillResponse> batchMoveCategory(SkillBatchActionRequest request) {
        validateBatchRequest(request);
        if (!StringUtils.hasText(request.getTargetCategoryCode())) {
            throw BusinessExceptions.badRequest("目标分类不能为空");
        }
        request.getSkillIds().forEach(skillId -> {
            SkillRecord record = skillSupportManager.requireSkill(skillId);
            record.setSkillCategory(request.getTargetCategoryCode().trim());
            SkillSnapshotDTO snapshot = skillSupportManager.parseSnapshot(record.getExt());
            snapshot.setSkillCategory(record.getSkillCategory());
            snapshot.setCategoryChain(List.of(toCategory(record.getSkillCategory())));
            record.setExt(skillSupportManager.toJson(snapshot));
            skillRecordService.updateById(record);
        });
        return listSkills();
    }

    /**
     * 批量发布技能。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SkillResponse> batchPublish(SkillBatchActionRequest request) {
        validateBatchRequest(request);
        request.getSkillIds().forEach(this::publishSkill);
        return listSkills();
    }

    /**
     * 批量下线技能。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SkillResponse> batchOffline(SkillBatchActionRequest request) {
        validateBatchRequest(request);
        request.getSkillIds().forEach(this::offlineSkill);
        return listSkills();
    }

    /**
     * 查询技能测试用例。
     */
    public List<SkillTestCaseResponse> listTestCases(Long skillId) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        return skillTestCaseRecordService.listBySkillId(record.getId(), record.getTenantId()).stream()
                .map(SkillAssembler::toTestCaseResponse)
                .toList();
    }

    /**
     * 创建技能测试用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillTestCaseResponse createTestCase(Long skillId, SkillTestCaseSaveRequest request) {
        SkillRecord record = skillSupportManager.requireSkill(skillId);
        validateTestCaseRequest(request);
        SkillTestCaseRecord testCase = new SkillTestCaseRecord();
        fillTestCase(testCase, record, request);
        skillTestCaseRecordService.save(testCase);
        return SkillAssembler.toTestCaseResponse(testCase);
    }

    /**
     * 更新技能测试用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillTestCaseResponse updateTestCase(Long testCaseId, SkillTestCaseSaveRequest request) {
        SkillTestCaseRecord testCase = requireTestCase(testCaseId);
        SkillRecord record = skillSupportManager.requireSkill(testCase.getSkillId());
        validateTestCaseRequest(request);
        fillTestCase(testCase, record, request);
        skillTestCaseRecordService.updateById(testCase);
        return SkillAssembler.toTestCaseResponse(testCase);
    }

    /**
     * 删除技能测试用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTestCase(Long testCaseId) {
        SkillTestCaseRecord testCase = requireTestCase(testCaseId);
        skillTestCaseRecordService.removeById(testCase.getId());
    }

    /**
     * 运行测试用例并写入调试日志。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillDebugResponse runTestCase(Long testCaseId) {
        SkillTestCaseRecord testCase = requireTestCase(testCaseId);
        SkillDebugRequest request = new SkillDebugRequest();
        request.setSkillId(testCase.getSkillId());
        request.setInputText(testCase.getInputText());
        request.setForcedIntent(testCase.getExpectedIntent());
        request.setSlotPayload(skillSupportManager.parseMap(testCase.getSlotPayloadJson()));
        request.setChannelCode(CommonTextUtils.defaultText(testCase.getChannelCode(), DEFAULT_CHANNEL_CODE));
        request.setLocale(CommonTextUtils.defaultText(testCase.getLocale(), DEFAULT_LOCALE));
        SkillDebugResponse response = executeDebug(request, SkillManagementConstants.LOG_SOURCE_TEST, testCase.getId());
        testCase.setLastRunStatus(Integer.valueOf(1).equals(response.getSuccessFlag()) ? "SUCCESS" : "FAILED");
        testCase.setLastRunDurationMs(response.getElapsedMs());
        testCase.setLastRunAt(LocalDateTime.now());
        testCase.setLastResultJson(skillSupportManager.toJson(response));
        skillTestCaseRecordService.updateById(testCase);
        return response;
    }

    /**
     * 在线调试技能并写入调试日志。
     */
    @Transactional(rollbackFor = Exception.class)
    public SkillDebugResponse debugSkill(SkillDebugRequest request) {
        return executeDebug(request, SkillManagementConstants.LOG_SOURCE_DEBUG, null);
    }

    /**
     * 查询技能执行日志。
     */
    public List<SkillExecutionLogResponse> listLogs(SkillLogQueryRequest request) {
        Long tenantId = skillSupportManager.getCurrentTenantId();
        List<SkillExecutionLogRecord> records = request != null && request.getSkillId() != null
                ? skillExecutionLogRecordService.listBySkillId(request.getSkillId(), tenantId)
                : skillExecutionLogRecordService.listByTenantId(tenantId);
        return records.stream()
                .filter(item -> request == null || !StringUtils.hasText(request.getSourceType()) || request.getSourceType().equals(item.getSourceType()))
                .filter(item -> request == null || request.getSuccessFlag() == null || request.getSuccessFlag().equals(item.getSuccessFlag()))
                .map(SkillAssembler::toLogResponse)
                .toList();
    }

    /**
     * 将实体转换成前端响应对象，并补充版本、用例和日志数量。
     */
    private SkillResponse toResponse(SkillRecord record) {
        Long tenantId = record.getTenantId();
        return SkillAssembler.toResponse(
                record,
                skillSupportManager.parseSnapshot(record.getExt()),
                skillVersionRecordService.listBySkillId(record.getId(), tenantId),
                skillTestCaseRecordService.listBySkillId(record.getId(), tenantId).size(),
                skillExecutionLogRecordService.listBySkillId(record.getId(), tenantId).size());
    }

    /**
     * 填充 SkillRecord 的通用字段。
     */
    private void fillRecord(SkillRecord record, SkillSaveRequest request, String publishStatus) {
        record.setSkillName(request.getSkillName().trim());
        record.setDescription(CommonTextUtils.trimToNull(request.getDescription()));
        record.setSkillType(CommonTextUtils.defaultText(request.getSkillType(), DEFAULT_SKILL_TYPE));
        record.setSkillCategory(CommonTextUtils.defaultText(request.getSkillCategory(), DEFAULT_SKILL_CATEGORY));
        record.setSkillStatus(CommonTextUtils.defaultText(request.getSkillStatus(), SkillManagementConstants.SKILL_STATUS_ENABLED));
        record.setPublishStatus(CommonTextUtils.defaultText(publishStatus, SkillManagementConstants.PUBLISH_STATUS_DRAFT));
        record.setVersionMode(CommonTextUtils.defaultText(request.getVersionMode(), SkillManagementConstants.VERSION_MODE_MANUAL));
        record.setSortWeight(request.getSortWeight() == null ? 100 : request.getSortWeight());
        record.setHotUpdateEnabled(defaultFlag(request.getHotUpdateEnabled()));
        record.setRemark(CommonTextUtils.trimToNull(request.getRemark()));
    }

    /**
     * 将请求组装成可持久化的技能快照。
     */
    private SkillSnapshotDTO toSnapshot(SkillSaveRequest request, String publishStatus) {
        return SkillSnapshotDTO.builder()
                .skillCode(CommonTextUtils.trimToNull(request.getSkillCode()))
                .skillName(CommonTextUtils.trimToNull(request.getSkillName()))
                .description(CommonTextUtils.trimToNull(request.getDescription()))
                .skillType(CommonTextUtils.defaultText(request.getSkillType(), DEFAULT_SKILL_TYPE))
                .skillCategory(CommonTextUtils.defaultText(request.getSkillCategory(), DEFAULT_SKILL_CATEGORY))
                .categoryChain(request.getCategoryChain() == null || request.getCategoryChain().isEmpty()
                        ? List.of(toCategory(CommonTextUtils.defaultText(request.getSkillCategory(), DEFAULT_SKILL_CATEGORY)))
                        : request.getCategoryChain())
                .tags(request.getTags() == null ? List.of() : request.getTags())
                .skillStatus(CommonTextUtils.defaultText(request.getSkillStatus(), SkillManagementConstants.SKILL_STATUS_ENABLED))
                .publishStatus(CommonTextUtils.defaultText(publishStatus, SkillManagementConstants.PUBLISH_STATUS_DRAFT))
                .versionCode(CommonTextUtils.trimToNull(request.getVersionCode()))
                .versionDescription(CommonTextUtils.trimToNull(request.getVersionDescription()))
                .versionMode(CommonTextUtils.defaultText(request.getVersionMode(), SkillManagementConstants.VERSION_MODE_MANUAL))
                .sortWeight(request.getSortWeight() == null ? 100 : request.getSortWeight())
                .hotUpdateEnabled(defaultFlag(request.getHotUpdateEnabled()))
                .observabilityConfig(request.getObservabilityConfig())
                .releaseConfig(request.getReleaseConfig())
                .batchConfig(request.getBatchConfig())
                .workflowConfig(request.getWorkflowConfig())
                .channelAdaptations(request.getChannelAdaptations() == null ? List.of() : request.getChannelAdaptations())
                .marketplaceConfig(request.getMarketplaceConfig())
                .remark(CommonTextUtils.trimToNull(request.getRemark()))
                .build();
    }

    /**
     * 创建版本快照记录。
     */
    private void createVersion(SkillRecord record, Integer versionNo, String versionStatus, String publishStatus) {
        SkillSnapshotDTO snapshot = skillSupportManager.parseSnapshot(record.getExt());
        SkillVersionRecord versionRecord = new SkillVersionRecord();
        versionRecord.setSkillId(record.getId());
        versionRecord.setSkillCode(record.getSkillCode());
        versionRecord.setSkillName(record.getSkillName());
        versionRecord.setTenantId(record.getTenantId());
        versionRecord.setVersionNo(versionNo);
        versionRecord.setVersionCode(snapshot.getVersionCode());
        versionRecord.setVersionDescription(snapshot.getVersionDescription());
        versionRecord.setVersionStatus(versionStatus);
        versionRecord.setPublishStatus(publishStatus);
        versionRecord.setReleaseStage(snapshot.getReleaseConfig() == null ? null : snapshot.getReleaseConfig().getReleaseStage());
        versionRecord.setSnapshotJson(record.getExt());
        skillVersionRecordService.save(versionRecord);
    }

    /**
     * 将当前版本标记为历史版本。
     */
    private void markCurrentVersionAsHistory(SkillRecord record) {
        skillVersionRecordService.listBySkillId(record.getId(), record.getTenantId()).forEach(version -> {
            if (SkillManagementConstants.VERSION_STATUS_CURRENT.equals(version.getVersionStatus())) {
                version.setVersionStatus(SkillManagementConstants.VERSION_STATUS_HISTORY);
                skillVersionRecordService.updateById(version);
            }
        });
    }

    /**
     * 更新技能和当前版本的发布状态。
     */
    private void updateSnapshotPublishStatus(SkillRecord record, String publishStatus) {
        SkillSnapshotDTO snapshot = skillSupportManager.parseSnapshot(record.getExt());
        snapshot.setPublishStatus(publishStatus);
        record.setExt(skillSupportManager.toJson(snapshot));
        skillRecordService.updateById(record);
    }

    /**
     * 更新当前版本发布状态。
     */
    private void updateCurrentVersionPublishStatus(SkillRecord record, String publishStatus) {
        SkillVersionRecord versionRecord = skillVersionRecordService.getBySkillIdAndVersionNo(
                record.getId(), record.getTenantId(), record.getCurrentVersionNo());
        if (versionRecord != null) {
            versionRecord.setPublishStatus(publishStatus);
            skillVersionRecordService.updateById(versionRecord);
        }
    }

    /**
     * 执行技能调试的轻量模拟，并统一生成执行日志。
     */
    private SkillDebugResponse executeDebug(SkillDebugRequest request, String sourceType, Long sourceId) {
        if (request == null || !StringUtils.hasText(request.getInputText())) {
            throw BusinessExceptions.badRequest("调试输入不能为空");
        }
        long startAt = System.currentTimeMillis();
        SkillRecord record = request.getSkillId() == null ? null : skillSupportManager.requireSkill(request.getSkillId());
        String matchedIntent = StringUtils.hasText(request.getForcedIntent())
                ? request.getForcedIntent().trim()
                : record == null ? "GENERAL_SKILL_MATCH" : record.getSkillCode();
        boolean success = record != null && SkillManagementConstants.SKILL_STATUS_ENABLED.equals(record.getSkillStatus());
        String resultText = success
                ? "技能 " + record.getSkillName() + " 已处理输入：" + request.getInputText().trim()
                : "未找到可用技能或技能未启用";
        SkillDebugResponse response = SkillDebugResponse.builder()
                .skillId(record == null ? null : record.getId())
                .skillCode(record == null ? null : record.getSkillCode())
                .matchedIntent(matchedIntent)
                .confidenceScore(success ? 0.92D : 0.18D)
                .successFlag(success ? 1 : 0)
                .responseText(success ? resultText : null)
                .failureReason(success ? null : resultText)
                .elapsedMs(Math.max(1L, System.currentTimeMillis() - startAt))
                .resolvedSlots(request.getSlotPayload() == null ? Map.of() : request.getSlotPayload())
                .contextPayload(request.getContextPayload() == null ? Map.of() : request.getContextPayload())
                .traceSteps(List.of(
                        SkillDebugTraceStepDTO.builder().stepName("参数接收").stepStatus("SUCCESS").detail("调试请求已解析").build(),
                        SkillDebugTraceStepDTO.builder().stepName("技能匹配").stepStatus(success ? "SUCCESS" : "FAILED").detail(resultText).build()))
                .build();
        saveExecutionLog(request, record, response, sourceType, sourceId);
        return response;
    }

    /**
     * 保存执行日志，便于页面查看调试和测试记录。
     */
    private void saveExecutionLog(
            SkillDebugRequest request,
            SkillRecord record,
            SkillDebugResponse response,
            String sourceType,
            Long sourceId
    ) {
        SkillExecutionLogRecord logRecord = new SkillExecutionLogRecord();
        logRecord.setSkillId(record == null ? null : record.getId());
        logRecord.setSkillCode(record == null ? null : record.getSkillCode());
        logRecord.setSkillName(record == null ? null : record.getSkillName());
        logRecord.setTenantId(skillSupportManager.getCurrentTenantId());
        logRecord.setSourceType(sourceType);
        logRecord.setSourceId(sourceId);
        logRecord.setTraceId(UUID.randomUUID().toString());
        logRecord.setSessionCode("skill-debug");
        logRecord.setChannelCode(CommonTextUtils.defaultText(request.getChannelCode(), DEFAULT_CHANNEL_CODE));
        logRecord.setLocale(CommonTextUtils.defaultText(request.getLocale(), DEFAULT_LOCALE));
        logRecord.setInputText(request.getInputText());
        logRecord.setMatchedIntent(response.getMatchedIntent());
        logRecord.setConfidenceScore(response.getConfidenceScore());
        logRecord.setSlotPayloadJson(skillSupportManager.toJson(request.getSlotPayload() == null ? Map.of() : request.getSlotPayload()));
        logRecord.setContextPayloadJson(skillSupportManager.toJson(request.getContextPayload() == null ? Map.of() : request.getContextPayload()));
        logRecord.setRequestPayloadJson(skillSupportManager.toJson(request));
        logRecord.setResponsePayloadJson(skillSupportManager.toJson(response));
        logRecord.setTracePayloadJson(skillSupportManager.toJson(response.getTraceSteps()));
        logRecord.setExecuteStatus(Integer.valueOf(1).equals(response.getSuccessFlag()) ? SkillManagementConstants.DEBUG_STATUS_MATCHED : SkillManagementConstants.DEBUG_STATUS_FAILED);
        logRecord.setSuccessFlag(response.getSuccessFlag());
        logRecord.setElapsedMs(response.getElapsedMs());
        logRecord.setFailureReason(response.getFailureReason());
        logRecord.setOperatorUserId(skillSupportManager.getCurrentUserId());
        logRecord.setOperatorUserName(skillSupportManager.getCurrentUserName());
        skillExecutionLogRecordService.save(logRecord);
    }

    /**
     * 填充测试用例实体。
     */
    private void fillTestCase(SkillTestCaseRecord testCase, SkillRecord skill, SkillTestCaseSaveRequest request) {
        testCase.setSkillId(skill.getId());
        testCase.setSkillCode(skill.getSkillCode());
        testCase.setCaseName(request.getCaseName().trim());
        testCase.setInputText(request.getInputText().trim());
        testCase.setSlotPayloadJson(skillSupportManager.toJson(request.getSlotPayload() == null ? Map.of() : request.getSlotPayload()));
        testCase.setExpectedIntent(CommonTextUtils.trimToNull(request.getExpectedIntent()));
        testCase.setExpectedSuccess(request.getExpectedSuccess() == null ? 1 : defaultFlag(request.getExpectedSuccess()));
        testCase.setExpectedResponseContains(CommonTextUtils.trimToNull(request.getExpectedResponseContains()));
        testCase.setChannelCode(CommonTextUtils.defaultText(request.getChannelCode(), DEFAULT_CHANNEL_CODE));
        testCase.setLocale(CommonTextUtils.defaultText(request.getLocale(), DEFAULT_LOCALE));
        testCase.setEnabled(request.getEnabled() == null ? 1 : defaultFlag(request.getEnabled()));
        testCase.setTenantId(skill.getTenantId());
    }

    /**
     * 复制源技能的测试用例。
     */
    private void duplicateTestCases(Long sourceSkillId, Long targetSkillId, String targetSkillCode) {
        Long tenantId = skillSupportManager.getCurrentTenantId();
        skillTestCaseRecordService.listBySkillId(sourceSkillId, tenantId).forEach(source -> {
            SkillTestCaseRecord copied = new SkillTestCaseRecord();
            copied.setSkillId(targetSkillId);
            copied.setSkillCode(targetSkillCode);
            copied.setCaseName(source.getCaseName());
            copied.setInputText(source.getInputText());
            copied.setSlotPayloadJson(source.getSlotPayloadJson());
            copied.setExpectedIntent(source.getExpectedIntent());
            copied.setExpectedSuccess(source.getExpectedSuccess());
            copied.setExpectedResponseContains(source.getExpectedResponseContains());
            copied.setChannelCode(source.getChannelCode());
            copied.setLocale(source.getLocale());
            copied.setEnabled(source.getEnabled());
            copied.setTenantId(tenantId);
            skillTestCaseRecordService.save(copied);
        });
    }

    /**
     * 用快照填充保存请求。
     */
    private void fillSaveRequestFromSnapshot(SkillSaveRequest request, SkillSnapshotDTO snapshot) {
        request.setSkillCode(snapshot.getSkillCode());
        request.setSkillName(snapshot.getSkillName());
        request.setDescription(snapshot.getDescription());
        request.setSkillType(snapshot.getSkillType());
        request.setSkillCategory(snapshot.getSkillCategory());
        request.setCategoryChain(snapshot.getCategoryChain());
        request.setTags(snapshot.getTags());
        request.setSkillStatus(snapshot.getSkillStatus());
        request.setSortWeight(snapshot.getSortWeight());
        request.setVersionCode(snapshot.getVersionCode());
        request.setVersionDescription(snapshot.getVersionDescription());
        request.setVersionMode(snapshot.getVersionMode());
        request.setHotUpdateEnabled(snapshot.getHotUpdateEnabled());
        request.setObservabilityConfig(snapshot.getObservabilityConfig());
        request.setReleaseConfig(snapshot.getReleaseConfig());
        request.setBatchConfig(snapshot.getBatchConfig());
        request.setWorkflowConfig(snapshot.getWorkflowConfig());
        request.setChannelAdaptations(snapshot.getChannelAdaptations());
        request.setMarketplaceConfig(snapshot.getMarketplaceConfig());
        request.setRemark(snapshot.getRemark());
    }

    /**
     * 要求指定版本存在。
     */
    private SkillVersionRecord requireVersion(SkillRecord record, Integer versionNo) {
        SkillVersionRecord version = skillVersionRecordService.getBySkillIdAndVersionNo(record.getId(), record.getTenantId(), versionNo);
        if (version == null) {
            throw BusinessExceptions.notFound("未找到技能版本：" + versionNo);
        }
        return version;
    }

    /**
     * 要求指定测试用例存在且属于当前租户。
     */
    private SkillTestCaseRecord requireTestCase(Long testCaseId) {
        SkillTestCaseRecord testCase = skillTestCaseRecordService.getById(testCaseId);
        if (testCase == null || !Objects.equals(testCase.getTenantId(), skillSupportManager.getCurrentTenantId())) {
            throw BusinessExceptions.notFound("未找到测试用例：" + testCaseId);
        }
        return testCase;
    }

    /**
     * 要求回收站中的技能存在。
     */
    private SkillRecord requireDeletedSkill(Long skillId) {
        SkillRecord record = skillRecordService.getById(skillId);
        if (record == null || !Objects.equals(record.getTenantId(), skillSupportManager.getCurrentTenantId()) || !Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw BusinessExceptions.notFound("未找到已删除技能：" + skillId);
        }
        return record;
    }

    /**
     * 校验技能保存请求。
     */
    private void validateSaveRequest(SkillSaveRequest request, boolean createMode) {
        if (request == null) {
            throw BusinessExceptions.badRequest("技能参数不能为空");
        }
        if (createMode && !StringUtils.hasText(request.getSkillCode())) {
            throw BusinessExceptions.badRequest("技能编码不能为空");
        }
        if (!StringUtils.hasText(request.getSkillName())) {
            throw BusinessExceptions.badRequest("技能名称不能为空");
        }
    }

    /**
     * 校验批量请求。
     */
    private void validateBatchRequest(SkillBatchActionRequest request) {
        if (request == null || request.getSkillIds() == null || request.getSkillIds().isEmpty()) {
            throw BusinessExceptions.badRequest("请选择要操作的技能");
        }
    }

    /**
     * 校验测试用例请求。
     */
    private void validateTestCaseRequest(SkillTestCaseSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getCaseName()) || !StringUtils.hasText(request.getInputText())) {
            throw BusinessExceptions.badRequest("测试用例名称和输入不能为空");
        }
    }

    /**
     * 生成版本差异摘要。
     */
    private String buildDiffSummary(String sourceSnapshotJson, String targetSnapshotJson) {
        if (Objects.equals(sourceSnapshotJson, targetSnapshotJson)) {
            return "两个版本快照一致";
        }
        return "两个版本快照存在差异，请查看 sourceSnapshotJson 与 targetSnapshotJson";
    }

    /**
     * 将标签名称转换成标签对象。
     */
    private SkillTagDTO toTag(String tagName) {
        SkillTagDTO tag = new SkillTagDTO();
        tag.setTagName(tagName.trim());
        tag.setTagCode(tagName.trim().toUpperCase().replace(" ", "_"));
        tag.setTagType("CUSTOM");
        tag.setColor("#6fa8ff");
        return tag;
    }

    /**
     * 将分类编码转换成分类对象。
     */
    private SkillCategoryDTO toCategory(String categoryCode) {
        SkillCategoryDTO category = new SkillCategoryDTO();
        category.setCategoryCode(categoryCode);
        category.setCategoryName(categoryCode);
        category.setCategoryLevel(1);
        return category;
    }

    /**
     * 统一处理 0/1 开关。
     */
    private Integer defaultFlag(Integer value) {
        return Integer.valueOf(1).equals(value) ? 1 : 0;
    }
}
