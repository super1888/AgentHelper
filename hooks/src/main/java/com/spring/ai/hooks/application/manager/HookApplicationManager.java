package com.spring.ai.hooks.application.manager;

import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.exception.BusinessExceptions;
import com.spring.ai.common.repository.enitiy.HookAgentBindingRecord;
import com.spring.ai.common.repository.enitiy.HookExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.HookRecord;
import com.spring.ai.common.repository.enitiy.HookTestCaseRecord;
import com.spring.ai.common.repository.enitiy.HookVersionRecord;
import com.spring.ai.common.repository.service.HookAgentBindingRecordService;
import com.spring.ai.common.repository.service.HookExecutionLogRecordService;
import com.spring.ai.common.repository.service.HookRecordService;
import com.spring.ai.common.repository.service.HookTestCaseRecordService;
import com.spring.ai.common.repository.service.HookVersionRecordService;
import com.spring.ai.common.utils.CommonTextUtils;
import com.spring.ai.hooks.application.assmbler.HookAssembler;
import com.spring.ai.hooks.config.HookManagementConstants;
import com.spring.ai.hooks.domain.dto.HookSnapshotDTO;
import com.spring.ai.hooks.domain.dto.HookTagDTO;
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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文件用途：Hook 管理应用服务
 * 核心职责：封装 Hook 的配置、发布、版本、绑定、调试、测试和日志能力
 */
@Component
public class HookApplicationManager {

    @Resource
    private HookRecordService hookRecordService;

    @Resource
    private HookVersionRecordService hookVersionRecordService;

    @Resource
    private HookTestCaseRecordService hookTestCaseRecordService;

    @Resource
    private HookExecutionLogRecordService hookExecutionLogRecordService;

    @Resource
    private HookAgentBindingRecordService hookAgentBindingRecordService;

    @Resource
    private HookSupportManager hookSupportManager;

    @Resource
    private HookCatalogRegistry hookCatalogRegistry;

    /**
     * 查询 Hook 列表。
     */
    public List<HookResponse> listHooks() {
        return hookRecordService.listByTenantId(hookSupportManager.getCurrentTenantId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 查询已删除 Hook 列表。
     */
    public List<HookResponse> listDeletedHooks() {
        return hookRecordService.listDeletedByTenantId(hookSupportManager.getCurrentTenantId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 查询 Hook 详情。
     */
    public HookResponse getHookDetail(Long hookId) {
        return toResponse(hookSupportManager.requireHook(hookId));
    }

    /**
     * 查询 Hook 内置模板目录。
     */
    public List<HookCatalogResponse> listCatalog() {
        return hookCatalogRegistry.listCatalog().stream()
                .map(HookAssembler::toCatalogResponse)
                .toList();
    }

    /**
     * 创建 Hook。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookResponse createHook(HookSaveRequest request) {
        validateSaveRequest(request, true);
        Long tenantId = hookSupportManager.getCurrentTenantId();
        if (hookRecordService.getByHookCode(tenantId, request.getHookCode()) != null) {
            throw BusinessExceptions.badRequest("Hook 编码已存在：" + request.getHookCode());
        }
        HookRecord record = new HookRecord();
        HookAssembler.mergeRecord(record, request);
        record.setPublishStatus(HookManagementConstants.PUBLISH_STATUS_DRAFT);
        record.setCurrentVersionNo(1);
        record.setLatestVersionNo(1);
        record.setDeletedFlag(0);
        record.setTenantId(tenantId);
        record.setOwnerUserId(hookSupportManager.getCurrentUserId());
        record.setOwnerUserName(hookSupportManager.getCurrentUserName());
        record.setExt(hookSupportManager.toJson(toSnapshot(request, HookManagementConstants.PUBLISH_STATUS_DRAFT)));
        hookRecordService.save(record);
        createVersion(record, 1, HookManagementConstants.VERSION_STATUS_CURRENT, HookManagementConstants.PUBLISH_STATUS_DRAFT);
        return getHookDetail(record.getId());
    }

    /**
     * 更新 Hook。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookResponse updateHook(Long hookId, HookSaveRequest request) {
        HookRecord record = hookSupportManager.requireHook(hookId);
        validateSaveRequest(request, false);
        HookRecord existed = hookRecordService.getByHookCode(record.getTenantId(), request.getHookCode());
        if (existed != null && !Objects.equals(existed.getId(), hookId)) {
            throw BusinessExceptions.badRequest("Hook 编码已存在：" + request.getHookCode());
        }
        markCurrentVersionAsHistory(record);
        HookAssembler.mergeRecord(record, request);
        int nextVersionNo = record.getLatestVersionNo() == null ? 1 : record.getLatestVersionNo() + 1;
        record.setCurrentVersionNo(nextVersionNo);
        record.setLatestVersionNo(nextVersionNo);
        record.setExt(hookSupportManager.toJson(toSnapshot(request, record.getPublishStatus())));
        hookRecordService.updateById(record);
        createVersion(record, nextVersionNo, HookManagementConstants.VERSION_STATUS_CURRENT, record.getPublishStatus());
        return getHookDetail(record.getId());
    }

    /**
     * 删除 Hook。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteHook(Long hookId) {
        HookRecord record = hookSupportManager.requireHook(hookId);
        record.setDeletedFlag(1);
        record.setPublishStatus(HookManagementConstants.PUBLISH_STATUS_OFFLINE);
        hookRecordService.updateById(record);
    }

    /**
     * 恢复已删除 Hook。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookResponse restoreHook(Long hookId) {
        HookRecord record = requireDeletedHook(hookId);
        record.setDeletedFlag(0);
        hookRecordService.updateById(record);
        return getHookDetail(record.getId());
    }

    /**
     * 发布 Hook。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookResponse publishHook(Long hookId) {
        HookRecord record = hookSupportManager.requireHook(hookId);
        record.setPublishStatus(HookManagementConstants.PUBLISH_STATUS_PUBLISHED);
        record.setHookStatus(HookManagementConstants.HOOK_STATUS_ENABLED);
        record.setPublishedVersionNo(record.getCurrentVersionNo());
        updateSnapshotPublishStatus(record, HookManagementConstants.PUBLISH_STATUS_PUBLISHED);
        updateCurrentVersionPublishStatus(record, HookManagementConstants.PUBLISH_STATUS_PUBLISHED);
        return getHookDetail(record.getId());
    }

    /**
     * 下线 Hook。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookResponse offlineHook(Long hookId) {
        HookRecord record = hookSupportManager.requireHook(hookId);
        record.setPublishStatus(HookManagementConstants.PUBLISH_STATUS_OFFLINE);
        updateSnapshotPublishStatus(record, HookManagementConstants.PUBLISH_STATUS_OFFLINE);
        updateCurrentVersionPublishStatus(record, HookManagementConstants.PUBLISH_STATUS_OFFLINE);
        return getHookDetail(record.getId());
    }

    /**
     * 打开热更新开关。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookResponse hotUpdateHook(Long hookId) {
        HookRecord record = hookSupportManager.requireHook(hookId);
        record.setHotUpdateEnabled(1);
        HookSnapshotDTO snapshot = hookSupportManager.parseSnapshot(record.getExt());
        snapshot.setHotUpdateEnabled(1);
        record.setExt(hookSupportManager.toJson(snapshot));
        hookRecordService.updateById(record);
        return getHookDetail(record.getId());
    }

    /**
     * 回滚 Hook 版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookResponse rollbackHook(Long hookId, HookVersionRollbackRequest request) {
        HookRecord record = hookSupportManager.requireHook(hookId);
        if (request == null || request.getTargetVersionNo() == null) {
            throw BusinessExceptions.badRequest("目标版本号不能为空");
        }
        HookVersionRecord targetVersion = requireVersion(record, request.getTargetVersionNo());
        HookSnapshotDTO snapshot = hookSupportManager.parseSnapshot(targetVersion.getSnapshotJson());
        if (StringUtils.hasText(request.getVersionDescription())) {
            snapshot.setVersionDescription(request.getVersionDescription().trim());
        }
        snapshot.setPublishStatus(record.getPublishStatus());
        markCurrentVersionAsHistory(record);
        fillRecordBySnapshot(record, snapshot);
        int nextVersionNo = record.getLatestVersionNo() == null ? 1 : record.getLatestVersionNo() + 1;
        record.setCurrentVersionNo(nextVersionNo);
        record.setLatestVersionNo(nextVersionNo);
        record.setExt(hookSupportManager.toJson(snapshot));
        hookRecordService.updateById(record);
        createVersion(record, nextVersionNo, HookManagementConstants.VERSION_STATUS_ROLLBACK, record.getPublishStatus());
        return getHookDetail(record.getId());
    }

    /**
     * 对比 Hook 版本。
     */
    public HookVersionCompareResponse compareVersions(Long hookId, HookVersionCompareRequest request) {
        HookRecord record = hookSupportManager.requireHook(hookId);
        if (request == null || request.getSourceVersionNo() == null || request.getTargetVersionNo() == null) {
            throw BusinessExceptions.badRequest("版本号不能为空");
        }
        HookVersionRecord source = requireVersion(record, request.getSourceVersionNo());
        HookVersionRecord target = requireVersion(record, request.getTargetVersionNo());
        return HookVersionCompareResponse.builder()
                .sourceVersionNo(source.getVersionNo())
                .targetVersionNo(target.getVersionNo())
                .sourceSnapshotJson(source.getSnapshotJson())
                .targetSnapshotJson(target.getSnapshotJson())
                .diffSummary(Objects.equals(source.getSnapshotJson(), target.getSnapshotJson())
                        ? "两个版本快照一致"
                        : "两个版本快照存在差异，请重点关注条件、运行时和安全配置")
                .build();
    }

    /**
     * 查询统计信息。
     */
    public HookStatisticsResponse statistics() {
        Long tenantId = hookSupportManager.getCurrentTenantId();
        List<HookRecord> records = hookRecordService.listByTenantId(tenantId);
        List<HookRecord> deletedRecords = hookRecordService.listDeletedByTenantId(tenantId);
        List<HookExecutionLogRecord> logs = hookExecutionLogRecordService.listByTenantId(tenantId);
        int totalBindingCount = records.stream()
                .mapToInt(record -> hookAgentBindingRecordService.listByHookId(record.getId(), tenantId).size())
                .sum();
        int totalTestCaseCount = records.stream()
                .mapToInt(record -> hookTestCaseRecordService.listByHookId(record.getId(), tenantId).size())
                .sum();
        int successLogCount = (int) logs.stream().filter(item -> Integer.valueOf(1).equals(item.getSuccessFlag())).count();
        int highRiskCount = (int) records.stream()
                .filter(item -> HookManagementConstants.RISK_LEVEL_HIGH.equals(item.getRiskLevel())
                        || HookManagementConstants.RISK_LEVEL_CRITICAL.equals(item.getRiskLevel()))
                .count();
        return HookStatisticsResponse.builder()
                .totalCount(records.size())
                .enabledCount((int) records.stream().filter(item -> HookManagementConstants.HOOK_STATUS_ENABLED.equals(item.getHookStatus())).count())
                .publishedCount((int) records.stream().filter(item -> HookManagementConstants.PUBLISH_STATUS_PUBLISHED.equals(item.getPublishStatus())).count())
                .hotUpdateEnabledCount((int) records.stream().filter(item -> Integer.valueOf(1).equals(item.getHotUpdateEnabled())).count())
                .deletedCount(deletedRecords.size())
                .highRiskCount(highRiskCount)
                .totalBindingCount(totalBindingCount)
                .totalTestCaseCount(totalTestCaseCount)
                .totalLogCount(logs.size())
                .successLogCount(successLogCount)
                .failureLogCount(logs.size() - successLogCount)
                .build();
    }

    /**
     * 批量修改 Hook 状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<HookResponse> batchUpdateStatus(HookBatchActionRequest request) {
        validateBatchRequest(request);
        if (!StringUtils.hasText(request.getHookStatus())) {
            throw BusinessExceptions.badRequest("目标状态不能为空");
        }
        request.getHookIds().forEach(hookId -> {
            HookRecord record = hookSupportManager.requireHook(hookId);
            record.setHookStatus(request.getHookStatus().trim());
            HookSnapshotDTO snapshot = hookSupportManager.parseSnapshot(record.getExt());
            snapshot.setHookStatus(record.getHookStatus());
            record.setExt(hookSupportManager.toJson(snapshot));
            hookRecordService.updateById(record);
        });
        return listHooks();
    }

    /**
     * 批量修改 Hook 阶段。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<HookResponse> batchUpdateStage(HookBatchActionRequest request) {
        validateBatchRequest(request);
        if (!StringUtils.hasText(request.getHookStage())) {
            throw BusinessExceptions.badRequest("目标阶段不能为空");
        }
        request.getHookIds().forEach(hookId -> {
            HookRecord record = hookSupportManager.requireHook(hookId);
            record.setHookStage(request.getHookStage().trim());
            HookSnapshotDTO snapshot = hookSupportManager.parseSnapshot(record.getExt());
            snapshot.setHookStage(record.getHookStage());
            record.setExt(hookSupportManager.toJson(snapshot));
            hookRecordService.updateById(record);
        });
        return listHooks();
    }

    /**
     * 批量修改风险等级。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<HookResponse> batchUpdateRisk(HookBatchActionRequest request) {
        validateBatchRequest(request);
        if (!StringUtils.hasText(request.getRiskLevel())) {
            throw BusinessExceptions.badRequest("目标风险等级不能为空");
        }
        request.getHookIds().forEach(hookId -> {
            HookRecord record = hookSupportManager.requireHook(hookId);
            record.setRiskLevel(request.getRiskLevel().trim());
            HookSnapshotDTO snapshot = hookSupportManager.parseSnapshot(record.getExt());
            snapshot.setRiskLevel(record.getRiskLevel());
            record.setExt(hookSupportManager.toJson(snapshot));
            hookRecordService.updateById(record);
        });
        return listHooks();
    }

    /**
     * 批量覆盖标签。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<HookResponse> batchUpdateTags(HookBatchActionRequest request) {
        validateBatchRequest(request);
        List<HookTagDTO> tags = request.getTagNames() == null ? List.of() : request.getTagNames().stream()
                .filter(StringUtils::hasText)
                .map(this::toTag)
                .toList();
        request.getHookIds().forEach(hookId -> {
            HookRecord record = hookSupportManager.requireHook(hookId);
            HookSnapshotDTO snapshot = hookSupportManager.parseSnapshot(record.getExt());
            snapshot.setTags(tags);
            record.setExt(hookSupportManager.toJson(snapshot));
            hookRecordService.updateById(record);
        });
        return listHooks();
    }

    /**
     * 批量删除 Hook。
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(HookBatchActionRequest request) {
        validateBatchRequest(request);
        request.getHookIds().forEach(this::deleteHook);
    }

    /**
     * 批量发布 Hook。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<HookResponse> batchPublish(HookBatchActionRequest request) {
        validateBatchRequest(request);
        request.getHookIds().forEach(this::publishHook);
        return listHooks();
    }

    /**
     * 批量下线 Hook。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<HookResponse> batchOffline(HookBatchActionRequest request) {
        validateBatchRequest(request);
        request.getHookIds().forEach(this::offlineHook);
        return listHooks();
    }

    /**
     * 查询日志。
     */
    public List<HookExecutionLogResponse> listLogs(HookLogQueryRequest request) {
        return hookExecutionLogRecordService
                .listByCondition(
                        hookSupportManager.getCurrentTenantId(),
                        request == null ? null : request.getHookId(),
                        request == null ? null : request.getSourceType(),
                        request == null ? null : request.getSuccessFlag())
                .stream()
                .map(HookAssembler::toLogResponse)
                .toList();
    }

    /**
     * 调试 Hook。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookDebugResponse debugHook(HookDebugRequest request) {
        if (request == null || request.getHookId() == null) {
            throw BusinessExceptions.badRequest("hookId 不能为空");
        }
        HookRecord record = hookSupportManager.requireHook(request.getHookId());
        String requestPayloadJson = StringUtils.hasText(request.getRequestPayloadJson())
                ? request.getRequestPayloadJson().trim()
                : CommonTextUtils.defaultText(hookSupportManager.parseSnapshot(record.getExt()).getTestPayloadJson(), "{}");
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> tracePayload = buildTracePayload(record, requestPayloadJson, request.getContextPayload());
            String responsePayloadJson = hookSupportManager.prettyJson(tracePayload);
            long elapsedMs = Math.max(1L, System.currentTimeMillis() - start);
            saveExecutionLog(record, request, requestPayloadJson, responsePayloadJson, null, elapsedMs, 1, request.getSourceType(), null);
            return HookAssembler.toDebugResponse(
                    record,
                    1,
                    HookManagementConstants.EXECUTE_STATUS_SUCCESS,
                    responsePayloadJson,
                    null,
                    elapsedMs,
                    requestPayloadJson,
                    tracePayload);
        } catch (Exception exception) {
            long elapsedMs = Math.max(1L, System.currentTimeMillis() - start);
            String failureReason = exception instanceof BusinessException ? exception.getMessage() : "Hook 调试失败";
            saveExecutionLog(record, request, requestPayloadJson, null, failureReason, elapsedMs, 0, request.getSourceType(), null);
            return HookAssembler.toDebugResponse(
                    record,
                    0,
                    HookManagementConstants.EXECUTE_STATUS_FAILED,
                    null,
                    failureReason,
                    elapsedMs,
                    requestPayloadJson,
                    Map.of("failureReason", failureReason));
        }
    }

    /**
     * 查询测试用例列表。
     */
    public List<HookTestCaseResponse> listTestCases(Long hookId) {
        HookRecord record = hookSupportManager.requireHook(hookId);
        return hookTestCaseRecordService.listByHookId(record.getId(), record.getTenantId()).stream()
                .map(HookAssembler::toTestCaseResponse)
                .toList();
    }

    /**
     * 创建测试用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookTestCaseResponse createTestCase(Long hookId, HookTestCaseSaveRequest request) {
        HookRecord record = hookSupportManager.requireHook(hookId);
        validateTestCaseRequest(request);
        HookTestCaseRecord testCaseRecord = new HookTestCaseRecord();
        HookAssembler.fillTestCaseRecord(
                testCaseRecord,
                record,
                request,
                hookSupportManager.toJson(defaultMap(request.getInputPayload())),
                hookSupportManager.toJson(defaultMap(request.getContextPayload())));
        hookTestCaseRecordService.save(testCaseRecord);
        return HookAssembler.toTestCaseResponse(testCaseRecord);
    }

    /**
     * 更新测试用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookTestCaseResponse updateTestCase(Long testCaseId, HookTestCaseSaveRequest request) {
        HookTestCaseRecord record = requireTestCase(testCaseId);
        validateTestCaseRequest(request);
        HookRecord hook = hookSupportManager.requireHook(record.getHookId());
        HookAssembler.fillTestCaseRecord(
                record,
                hook,
                request,
                hookSupportManager.toJson(defaultMap(request.getInputPayload())),
                hookSupportManager.toJson(defaultMap(request.getContextPayload())));
        hookTestCaseRecordService.updateById(record);
        return HookAssembler.toTestCaseResponse(record);
    }

    /**
     * 删除测试用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTestCase(Long testCaseId) {
        HookTestCaseRecord record = requireTestCase(testCaseId);
        hookTestCaseRecordService.removeById(record.getId());
    }

    /**
     * 运行测试用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookDebugResponse runTestCase(Long testCaseId) {
        HookTestCaseRecord testCase = requireTestCase(testCaseId);
        HookDebugRequest request = new HookDebugRequest();
        request.setHookId(testCase.getHookId());
        request.setRequestPayloadJson(testCase.getInputPayloadJson());
        request.setContextPayload(hookSupportManager.parseMap(testCase.getContextPayloadJson()));
        request.setSourceType(HookManagementConstants.SOURCE_TYPE_TEST);
        HookDebugResponse response = debugHook(request);
        testCase.setLastRunStatus(response.getExecuteStatus());
        testCase.setLastRunDurationMs(response.getElapsedMs());
        testCase.setLastRunAt(LocalDateTime.now());
        testCase.setLastResultJson(response.getResponsePayloadJson());
        hookTestCaseRecordService.updateById(testCase);
        return response;
    }

    /**
     * 查询绑定列表。
     */
    public List<HookBindingResponse> listBindings(Long hookId) {
        HookRecord record = hookSupportManager.requireHook(hookId);
        return hookAgentBindingRecordService.listByHookId(record.getId(), record.getTenantId()).stream()
                .map(HookAssembler::toBindingResponse)
                .toList();
    }

    /**
     * 创建绑定。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookBindingResponse createBinding(Long hookId, HookBindingSaveRequest request) {
        HookRecord hook = hookSupportManager.requireHook(hookId);
        validateBindingRequest(request);
        HookAgentBindingRecord record = new HookAgentBindingRecord();
        HookAssembler.fillBindingRecord(record, hook, request);
        hookAgentBindingRecordService.save(record);
        return HookAssembler.toBindingResponse(record);
    }

    /**
     * 更新绑定。
     */
    @Transactional(rollbackFor = Exception.class)
    public HookBindingResponse updateBinding(Long bindingId, HookBindingSaveRequest request) {
        HookAgentBindingRecord record = requireBinding(bindingId);
        validateBindingRequest(request);
        HookRecord hook = hookSupportManager.requireHook(record.getHookId());
        HookAssembler.fillBindingRecord(record, hook, request);
        hookAgentBindingRecordService.updateById(record);
        return HookAssembler.toBindingResponse(record);
    }

    /**
     * 删除绑定。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteBinding(Long bindingId) {
        HookAgentBindingRecord record = requireBinding(bindingId);
        hookAgentBindingRecordService.removeById(record.getId());
    }

    private HookResponse toResponse(HookRecord record) {
        HookSnapshotDTO snapshot = hookSupportManager.parseSnapshot(record.getExt());
        return HookAssembler.toResponse(
                record,
                snapshot,
                hookSupportManager.countBindings(record.getId()),
                hookSupportManager.countTestCases(record.getId()),
                hookSupportManager.countLogs(record.getId()),
                hookVersionRecordService.listByHookId(record.getId(), record.getTenantId()));
    }

    private HookSnapshotDTO toSnapshot(HookSaveRequest request, String publishStatus) {
        return HookSnapshotDTO.builder()
                .hookCode(CommonTextUtils.trimToNull(request.getHookCode()))
                .hookName(CommonTextUtils.trimToNull(request.getHookName()))
                .description(CommonTextUtils.trimToNull(request.getDescription()))
                .hookType(CommonTextUtils.defaultText(request.getHookType(), "AGENT"))
                .hookStage(CommonTextUtils.defaultText(request.getHookStage(), "PRE_MODEL"))
                .hookStatus(CommonTextUtils.defaultText(request.getHookStatus(), HookManagementConstants.HOOK_STATUS_ENABLED))
                .publishStatus(CommonTextUtils.defaultText(publishStatus, HookManagementConstants.PUBLISH_STATUS_DRAFT))
                .riskLevel(CommonTextUtils.defaultText(request.getRiskLevel(), HookManagementConstants.RISK_LEVEL_LOW))
                .triggerMode(CommonTextUtils.defaultText(request.getTriggerMode(), "SYNC"))
                .failStrategy(CommonTextUtils.defaultText(request.getFailStrategy(), "CONTINUE"))
                .sortWeight(request.getSortWeight() == null ? 100 : request.getSortWeight())
                .timeoutMs(request.getTimeoutMs() == null ? 10000 : request.getTimeoutMs())
                .hotUpdateEnabled(defaultFlag(request.getHotUpdateEnabled()))
                .versionCode(CommonTextUtils.trimToNull(request.getVersionCode()))
                .versionDescription(CommonTextUtils.trimToNull(request.getVersionDescription()))
                .builtinHookKey(CommonTextUtils.trimToNull(request.getBuiltinHookKey()))
                .scriptLanguage(CommonTextUtils.defaultText(request.getScriptLanguage(), "JAVA"))
                .tags(request.getTags() == null ? List.of() : request.getTags().stream().map(this::toTag).toList())
                .targetChannels(defaultList(request.getTargetChannels()))
                .targetEnvironments(defaultList(request.getTargetEnvironments()))
                .targetAgentCodes(defaultList(request.getTargetAgentCodes()))
                .targetModelCodes(defaultList(request.getTargetModelCodes()))
                .conditionConfig(defaultMap(request.getConditionConfig()))
                .runtimeConfig(defaultMap(request.getRuntimeConfig()))
                .securityConfig(defaultMap(request.getSecurityConfig()))
                .observabilityConfig(defaultMap(request.getObservabilityConfig()))
                .degradationConfig(defaultMap(request.getDegradationConfig()))
                .scriptContent(CommonTextUtils.trimToNull(request.getScriptContent()))
                .testPayloadJson(CommonTextUtils.trimToNull(request.getTestPayloadJson()))
                .remark(CommonTextUtils.trimToNull(request.getRemark()))
                .build();
    }

    private void createVersion(HookRecord record, Integer versionNo, String versionStatus, String publishStatus) {
        HookSnapshotDTO snapshot = hookSupportManager.parseSnapshot(record.getExt());
        HookVersionRecord version = new HookVersionRecord();
        version.setHookId(record.getId());
        version.setHookCode(record.getHookCode());
        version.setHookName(record.getHookName());
        version.setTenantId(record.getTenantId());
        version.setVersionNo(versionNo);
        version.setVersionCode(snapshot.getVersionCode());
        version.setVersionDescription(snapshot.getVersionDescription());
        version.setVersionStatus(versionStatus);
        version.setPublishStatus(publishStatus);
        version.setSnapshotJson(record.getExt());
        hookVersionRecordService.save(version);
    }

    private void markCurrentVersionAsHistory(HookRecord record) {
        hookVersionRecordService.listByHookId(record.getId(), record.getTenantId()).forEach(item -> {
            if (HookManagementConstants.VERSION_STATUS_CURRENT.equals(item.getVersionStatus())) {
                item.setVersionStatus(HookManagementConstants.VERSION_STATUS_HISTORY);
                hookVersionRecordService.updateById(item);
            }
        });
    }

    private void updateSnapshotPublishStatus(HookRecord record, String publishStatus) {
        HookSnapshotDTO snapshot = hookSupportManager.parseSnapshot(record.getExt());
        snapshot.setPublishStatus(publishStatus);
        record.setExt(hookSupportManager.toJson(snapshot));
        hookRecordService.updateById(record);
    }

    private void updateCurrentVersionPublishStatus(HookRecord record, String publishStatus) {
        HookVersionRecord currentVersion = hookVersionRecordService.getByHookIdAndVersionNo(record.getId(), record.getTenantId(), record.getCurrentVersionNo());
        if (currentVersion != null) {
            currentVersion.setPublishStatus(publishStatus);
            hookVersionRecordService.updateById(currentVersion);
        }
    }

    private HookVersionRecord requireVersion(HookRecord record, Integer versionNo) {
        HookVersionRecord version = hookVersionRecordService.getByHookIdAndVersionNo(record.getId(), record.getTenantId(), versionNo);
        if (version == null) {
            throw BusinessExceptions.notFound("未找到 Hook 版本：" + versionNo);
        }
        return version;
    }

    private HookRecord requireDeletedHook(Long hookId) {
        HookRecord record = hookRecordService.getById(hookId);
        if (record == null || !Objects.equals(record.getTenantId(), hookSupportManager.getCurrentTenantId()) || !Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw BusinessExceptions.notFound("未找到已删除 Hook：" + hookId);
        }
        return record;
    }

    private HookTestCaseRecord requireTestCase(Long testCaseId) {
        HookTestCaseRecord record = hookTestCaseRecordService.getById(testCaseId);
        if (record == null || !Objects.equals(record.getTenantId(), hookSupportManager.getCurrentTenantId())) {
            throw BusinessExceptions.notFound("未找到 Hook 测试用例：" + testCaseId);
        }
        return record;
    }

    private HookAgentBindingRecord requireBinding(Long bindingId) {
        HookAgentBindingRecord record = hookAgentBindingRecordService.getById(bindingId);
        if (record == null || !Objects.equals(record.getTenantId(), hookSupportManager.getCurrentTenantId())) {
            throw BusinessExceptions.notFound("未找到 Hook 绑定：" + bindingId);
        }
        return record;
    }

    private void validateSaveRequest(HookSaveRequest request, boolean createMode) {
        if (request == null) throw BusinessExceptions.badRequest("Hook 请求不能为空");
        if (createMode && !StringUtils.hasText(request.getHookCode())) throw BusinessExceptions.badRequest("hookCode 不能为空");
        if (!StringUtils.hasText(request.getHookName())) throw BusinessExceptions.badRequest("hookName 不能为空");
        if (!StringUtils.hasText(request.getHookType())) throw BusinessExceptions.badRequest("hookType 不能为空");
        if (!StringUtils.hasText(request.getHookStage())) throw BusinessExceptions.badRequest("hookStage 不能为空");
        if (!StringUtils.hasText(request.getHookStatus())) throw BusinessExceptions.badRequest("hookStatus 不能为空");
        if (!StringUtils.hasText(request.getRiskLevel())) throw BusinessExceptions.badRequest("riskLevel 不能为空");
        if (!StringUtils.hasText(request.getTriggerMode())) throw BusinessExceptions.badRequest("triggerMode 不能为空");
        if (!StringUtils.hasText(request.getFailStrategy())) throw BusinessExceptions.badRequest("failStrategy 不能为空");
        if (request.getTimeoutMs() == null || request.getTimeoutMs() <= 0) throw BusinessExceptions.badRequest("timeoutMs 必须大于 0");
    }

    private void validateBatchRequest(HookBatchActionRequest request) {
        if (request == null || request.getHookIds() == null || request.getHookIds().isEmpty()) throw BusinessExceptions.badRequest("请选择要操作的 Hook");
    }

    private void validateTestCaseRequest(HookTestCaseSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getCaseName())) throw BusinessExceptions.badRequest("测试用例名称不能为空");
    }

    private void validateBindingRequest(HookBindingSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getBindingName()) || !StringUtils.hasText(request.getBindingScope())) {
            throw BusinessExceptions.badRequest("绑定名称和绑定范围不能为空");
        }
    }

    private void fillRecordBySnapshot(HookRecord record, HookSnapshotDTO snapshot) {
        record.setHookName(CommonTextUtils.defaultText(snapshot.getHookName(), record.getHookName()));
        record.setDescription(CommonTextUtils.trimToNull(snapshot.getDescription()));
        record.setHookType(CommonTextUtils.defaultText(snapshot.getHookType(), record.getHookType()));
        record.setHookStage(CommonTextUtils.defaultText(snapshot.getHookStage(), record.getHookStage()));
        record.setHookStatus(CommonTextUtils.defaultText(snapshot.getHookStatus(), record.getHookStatus()));
        record.setRiskLevel(CommonTextUtils.defaultText(snapshot.getRiskLevel(), record.getRiskLevel()));
        record.setTriggerMode(CommonTextUtils.defaultText(snapshot.getTriggerMode(), record.getTriggerMode()));
        record.setFailStrategy(CommonTextUtils.defaultText(snapshot.getFailStrategy(), record.getFailStrategy()));
        record.setSortWeight(snapshot.getSortWeight());
        record.setTimeoutMs(snapshot.getTimeoutMs());
        record.setHotUpdateEnabled(defaultFlag(snapshot.getHotUpdateEnabled()));
        record.setBuiltinHookKey(CommonTextUtils.trimToNull(snapshot.getBuiltinHookKey()));
        record.setScriptLanguage(CommonTextUtils.defaultText(snapshot.getScriptLanguage(), record.getScriptLanguage()));
        record.setRemark(CommonTextUtils.trimToNull(snapshot.getRemark()));
    }

    private HookTagDTO toTag(String tagName) {
        HookTagDTO tag = new HookTagDTO();
        tag.setTagCode(tagName.trim().toUpperCase().replace(" ", "_"));
        tag.setTagName(tagName.trim());
        tag.setColor("#6fd7ff");
        return tag;
    }

    private void saveExecutionLog(HookRecord record, HookDebugRequest request, String requestPayloadJson, String responsePayloadJson, String failureReason, Long elapsedMs, Integer successFlag, String sourceType, Long sourceId) {
        HookExecutionLogRecord log = new HookExecutionLogRecord();
        log.setHookId(record.getId());
        log.setHookCode(record.getHookCode());
        log.setHookName(record.getHookName());
        log.setTenantId(record.getTenantId());
        log.setSourceType(CommonTextUtils.defaultText(sourceType, HookManagementConstants.SOURCE_TYPE_DEBUG));
        log.setSourceId(sourceId);
        log.setTraceId(UUID.randomUUID().toString());
        log.setAgentCode(CommonTextUtils.trimToNull(request == null ? null : request.getAgentCode()));
        log.setSessionCode(CommonTextUtils.trimToNull(request == null ? null : request.getSessionCode()));
        log.setRequestPayloadJson(requestPayloadJson);
        log.setContextPayloadJson(hookSupportManager.toJson(request == null ? Map.of() : defaultMap(request.getContextPayload())));
        log.setResponsePayloadJson(responsePayloadJson);
        log.setExecuteStatus(Integer.valueOf(1).equals(successFlag) ? HookManagementConstants.EXECUTE_STATUS_SUCCESS : HookManagementConstants.EXECUTE_STATUS_FAILED);
        log.setSuccessFlag(successFlag);
        log.setElapsedMs(elapsedMs);
        log.setFailureReason(failureReason);
        log.setOperatorUserId(hookSupportManager.getCurrentUserId());
        log.setOperatorUserName(hookSupportManager.getCurrentUserName());
        hookExecutionLogRecordService.save(log);
    }

    private Map<String, Object> buildTracePayload(HookRecord record, String requestPayloadJson, Map<String, Object> contextPayload) {
        HookSnapshotDTO snapshot = hookSupportManager.parseSnapshot(record.getExt());
        Map<String, Object> tracePayload = new LinkedHashMap<>();
        tracePayload.put("hookCode", record.getHookCode());
        tracePayload.put("hookName", record.getHookName());
        tracePayload.put("hookStage", record.getHookStage());
        tracePayload.put("hookType", record.getHookType());
        tracePayload.put("triggerMode", record.getTriggerMode());
        tracePayload.put("riskLevel", record.getRiskLevel());
        tracePayload.put("failStrategy", record.getFailStrategy());
        tracePayload.put("targetAgents", defaultList(snapshot.getTargetAgentCodes()));
        tracePayload.put("targetModels", defaultList(snapshot.getTargetModelCodes()));
        tracePayload.put("conditionConfig", defaultMap(snapshot.getConditionConfig()));
        tracePayload.put("runtimeConfig", defaultMap(snapshot.getRuntimeConfig()));
        tracePayload.put("securityConfig", defaultMap(snapshot.getSecurityConfig()));
        tracePayload.put("requestPayload", hookSupportManager.parseMap(requestPayloadJson));
        tracePayload.put("contextPayload", defaultMap(contextPayload));
        tracePayload.put("decision", Integer.valueOf(1).equals(record.getHotUpdateEnabled()) ? "HOT_UPDATE_READY" : "EXECUTED");
        tracePayload.put("message", "Hook 配置校验通过并生成调试结果");
        return tracePayload;
    }

    private Integer defaultFlag(Integer value) {
        return Integer.valueOf(1).equals(value) ? 1 : 0;
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(StringUtils::hasText).map(String::trim).toList();
    }

    private Map<String, Object> defaultMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }
}
