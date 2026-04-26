package com.spring.ai.interceptors.application.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring.ai.common.exception.BusinessExceptions;
import com.spring.ai.common.repository.enitiy.InterceptorAgentBindingRecord;
import com.spring.ai.common.repository.enitiy.InterceptorExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.InterceptorRecord;
import com.spring.ai.common.repository.enitiy.InterceptorTestCaseRecord;
import com.spring.ai.common.repository.enitiy.InterceptorVersionRecord;
import com.spring.ai.common.repository.service.InterceptorAgentBindingRecordService;
import com.spring.ai.common.repository.service.InterceptorExecutionLogRecordService;
import com.spring.ai.common.repository.service.InterceptorRecordService;
import com.spring.ai.common.repository.service.InterceptorTestCaseRecordService;
import com.spring.ai.common.repository.service.InterceptorVersionRecordService;
import com.spring.ai.common.utils.CommonJsonUtils;
import com.spring.ai.common.utils.CommonTextUtils;
import com.spring.ai.interceptors.application.assmbler.InterceptorAssembler;
import com.spring.ai.interceptors.config.InterceptorManagementConstants;
import com.spring.ai.interceptors.domain.dto.InterceptorSnapshotDTO;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文件用途：Interceptor 管理应用服务
 * 核心职责：封装拦截器配置、发布、版本、绑定、调试、测试和日志能力
 */
@Component
public class InterceptorApplicationManager {

    @Resource
    private InterceptorRecordService interceptorRecordService;

    @Resource
    private InterceptorVersionRecordService interceptorVersionRecordService;

    @Resource
    private InterceptorTestCaseRecordService interceptorTestCaseRecordService;

    @Resource
    private InterceptorExecutionLogRecordService interceptorExecutionLogRecordService;

    @Resource
    private InterceptorAgentBindingRecordService interceptorAgentBindingRecordService;

    @Resource
    private InterceptorSupportManager interceptorSupportManager;

    @Resource
    private InterceptorCatalogRegistry interceptorCatalogRegistry;

    @Resource
    private CommonJsonUtils commonJsonUtils;


    /**
     * 查询拦截器列表。
     */
    public List<InterceptorResponse> listInterceptors() {
        return interceptorRecordService.listByTenantId(interceptorSupportManager.getCurrentTenantId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 查询已删除拦截器列表。
     */
    public List<InterceptorResponse> listDeletedInterceptors() {
        return interceptorRecordService.listDeletedByTenantId(interceptorSupportManager.getCurrentTenantId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 查询拦截器详情。
     */
    public InterceptorResponse getInterceptorDetail(Long interceptorId) {
        return toResponse(interceptorSupportManager.requireInterceptor(interceptorId));
    }

    /**
     * 查询内置拦截器模板目录。
     */
    public List<InterceptorCatalogResponse> listCatalog() {
        return interceptorCatalogRegistry.listCatalog().stream()
                .map(InterceptorAssembler::toCatalogResponse)
                .toList();
    }

    /**
     * 创建拦截器。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterceptorResponse createInterceptor(InterceptorSaveRequest request) {
        validateSaveRequest(request, true);
        Long tenantId = interceptorSupportManager.getCurrentTenantId();
        if (interceptorRecordService.getByInterceptorCode(tenantId, request.getInterceptorCode()) != null) {
            throw BusinessExceptions.badRequest("Interceptor 编码已存在：" + request.getInterceptorCode());
        }

        InterceptorRecord record = new InterceptorRecord();
        InterceptorAssembler.mergeRecord(record, request);
        record.setPublishStatus(InterceptorManagementConstants.PUBLISH_STATUS_DRAFT);
        record.setCurrentVersionNo(1);
        record.setLatestVersionNo(1);
        record.setDeletedFlag(0);
        record.setTenantId(tenantId);
        record.setOwnerUserId(interceptorSupportManager.getCurrentUserId());
        record.setOwnerUserName(interceptorSupportManager.getCurrentUserName());
        record.setExt(interceptorSupportManager.toJson(toSnapshot(request, InterceptorManagementConstants.PUBLISH_STATUS_DRAFT)));
        interceptorRecordService.save(record);
        createVersion(record, 1, InterceptorManagementConstants.VERSION_STATUS_CURRENT, InterceptorManagementConstants.PUBLISH_STATUS_DRAFT);
        return getInterceptorDetail(record.getId());
    }

    /**
     * 更新拦截器并生成新版本。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterceptorResponse updateInterceptor(Long interceptorId, InterceptorSaveRequest request) {
        InterceptorRecord record = interceptorSupportManager.requireInterceptor(interceptorId);
        validateSaveRequest(request, false);
        InterceptorRecord existed = interceptorRecordService.getByInterceptorCode(record.getTenantId(), request.getInterceptorCode());
        if (existed != null && !Objects.equals(existed.getId(), interceptorId)) {
            throw BusinessExceptions.badRequest("Interceptor 编码已存在：" + request.getInterceptorCode());
        }

        markCurrentVersionAsHistory(record);
        InterceptorAssembler.mergeRecord(record, request);
        int nextVersionNo = record.getLatestVersionNo() == null ? 1 : record.getLatestVersionNo() + 1;
        record.setCurrentVersionNo(nextVersionNo);
        record.setLatestVersionNo(nextVersionNo);
        record.setExt(interceptorSupportManager.toJson(toSnapshot(request, record.getPublishStatus())));
        interceptorRecordService.updateById(record);
        createVersion(record, nextVersionNo, InterceptorManagementConstants.VERSION_STATUS_CURRENT, record.getPublishStatus());
        return getInterceptorDetail(record.getId());
    }

    /**
     * 删除拦截器。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteInterceptor(Long interceptorId) {
        InterceptorRecord record = interceptorSupportManager.requireInterceptor(interceptorId);
        record.setDeletedFlag(1);
        record.setPublishStatus(InterceptorManagementConstants.PUBLISH_STATUS_OFFLINE);
        interceptorRecordService.updateById(record);
    }

    /**
     * 恢复已删除拦截器。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterceptorResponse restoreInterceptor(Long interceptorId) {
        InterceptorRecord record = requireDeletedInterceptor(interceptorId);
        record.setDeletedFlag(0);
        interceptorRecordService.updateById(record);
        return getInterceptorDetail(record.getId());
    }

    /**
     * 发布拦截器。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterceptorResponse publishInterceptor(Long interceptorId) {
        InterceptorRecord record = interceptorSupportManager.requireInterceptor(interceptorId);
        record.setPublishStatus(InterceptorManagementConstants.PUBLISH_STATUS_PUBLISHED);
        record.setInterceptorStatus(InterceptorManagementConstants.INTERCEPTOR_STATUS_ENABLED);
        record.setPublishedVersionNo(record.getCurrentVersionNo());
        updateSnapshotPublishStatus(record, InterceptorManagementConstants.PUBLISH_STATUS_PUBLISHED);
        updateCurrentVersionPublishStatus(record, InterceptorManagementConstants.PUBLISH_STATUS_PUBLISHED);
        return getInterceptorDetail(record.getId());
    }

    /**
     * 下线拦截器。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterceptorResponse offlineInterceptor(Long interceptorId) {
        InterceptorRecord record = interceptorSupportManager.requireInterceptor(interceptorId);
        record.setPublishStatus(InterceptorManagementConstants.PUBLISH_STATUS_OFFLINE);
        updateSnapshotPublishStatus(record, InterceptorManagementConstants.PUBLISH_STATUS_OFFLINE);
        updateCurrentVersionPublishStatus(record, InterceptorManagementConstants.PUBLISH_STATUS_OFFLINE);
        return getInterceptorDetail(record.getId());
    }

    /**
     * 打开热更新开关。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterceptorResponse hotUpdateInterceptor(Long interceptorId) {
        InterceptorRecord record = interceptorSupportManager.requireInterceptor(interceptorId);
        record.setHotUpdateEnabled(1);
        InterceptorSnapshotDTO snapshot = interceptorSupportManager.parseSnapshot(record.getExt());
        snapshot.setHotUpdateEnabled(1);
        record.setExt(interceptorSupportManager.toJson(snapshot));
        interceptorRecordService.updateById(record);
        return getInterceptorDetail(record.getId());
    }

    /**
     * 查询统计信息。
     */
    public InterceptorStatisticsResponse statistics() {
        Long tenantId = interceptorSupportManager.getCurrentTenantId();
        List<InterceptorRecord> records = interceptorRecordService.listByTenantId(tenantId);
        List<InterceptorRecord> deletedRecords = interceptorRecordService.listDeletedByTenantId(tenantId);
        List<InterceptorExecutionLogRecord> logs = interceptorExecutionLogRecordService.listByTenantId(tenantId);
        int totalBindingCount = records.stream()
                .mapToInt(record -> interceptorAgentBindingRecordService.listByInterceptorId(record.getId(), tenantId).size())
                .sum();
        int totalTestCaseCount = records.stream()
                .mapToInt(record -> interceptorTestCaseRecordService.listByInterceptorId(record.getId(), tenantId).size())
                .sum();
        int successLogCount = (int) logs.stream().filter(item -> Integer.valueOf(1).equals(item.getSuccessFlag())).count();
        int highRiskCount = (int) records.stream()
                .filter(item -> InterceptorManagementConstants.RISK_LEVEL_HIGH.equals(item.getRiskLevel())
                        || InterceptorManagementConstants.RISK_LEVEL_CRITICAL.equals(item.getRiskLevel()))
                .count();
        return InterceptorStatisticsResponse.builder()
                .totalCount(records.size())
                .enabledCount((int) records.stream().filter(item -> InterceptorManagementConstants.INTERCEPTOR_STATUS_ENABLED.equals(item.getInterceptorStatus())).count())
                .publishedCount((int) records.stream().filter(item -> InterceptorManagementConstants.PUBLISH_STATUS_PUBLISHED.equals(item.getPublishStatus())).count())
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
     * 调试拦截器。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterceptorDebugResponse debugInterceptor(InterceptorDebugRequest request) {
        if (request == null || request.getInterceptorId() == null) {
            throw BusinessExceptions.badRequest("interceptorId 不能为空");
        }
        InterceptorRecord record = interceptorSupportManager.requireInterceptor(request.getInterceptorId());
        InterceptorSnapshotDTO snapshot = interceptorSupportManager.parseSnapshot(record.getExt());
        String sourceType = StringUtils.hasText(request.getSourceType())
                ? request.getSourceType().trim()
                : InterceptorManagementConstants.SOURCE_TYPE_DEBUG;
        String requestPayloadJson = StringUtils.hasText(request.getRequestPayloadJson())
                ? request.getRequestPayloadJson()
                : snapshot.getTestPayloadJson();
        long startedAt = System.currentTimeMillis();
        try {
            Map<String, Object> requestPayload = interceptorSupportManager.parseMap(requestPayloadJson);
            SimulatedResult result = simulateInterceptor(record, snapshot, requestPayload, commonJsonUtils.safeMap(request.getContextPayload()));
            Long elapsedMs = System.currentTimeMillis() - startedAt;
            InterceptorDebugResponse response = InterceptorAssembler.toDebugResponse(
                    record,
                    result.successFlag(),
                    result.executeStatus(),
                    interceptorSupportManager.prettyJson(result.responsePayload()),
                    result.failureReason(),
                    elapsedMs,
                    requestPayloadJson,
                    result.tracePayload()
            );
            saveExecutionLog(record, request, sourceType, null, response);
            return response;
        } catch (RuntimeException e) {
            Long elapsedMs = System.currentTimeMillis() - startedAt;
            InterceptorDebugResponse response = InterceptorAssembler.toDebugResponse(
                    record,
                    0,
                    InterceptorManagementConstants.EXECUTE_STATUS_FAILED,
                    null,
                    e.getMessage(),
                    elapsedMs,
                    requestPayloadJson,
                    Map.of("errorType", e.getClass().getSimpleName())
            );
            saveExecutionLog(record, request, sourceType, null, response);
            return response;
        }
    }

    /**
     * 查询日志列表。
     */
    public List<InterceptorExecutionLogResponse> listLogs(InterceptorLogQueryRequest request) {
        Long tenantId = interceptorSupportManager.getCurrentTenantId();
        Long interceptorId = request == null ? null : request.getInterceptorId();
        String sourceType = request == null ? null : request.getSourceType();
        Integer successFlag = request == null ? null : request.getSuccessFlag();
        return interceptorExecutionLogRecordService.listByCondition(tenantId, interceptorId, sourceType, successFlag)
                .stream()
                .map(InterceptorAssembler::toLogResponse)
                .toList();
    }

    /**
     * 查询测试用例。
     */
    public List<InterceptorTestCaseResponse> listTestCases(Long interceptorId) {
        InterceptorRecord record = interceptorSupportManager.requireInterceptor(interceptorId);
        return interceptorTestCaseRecordService.listByInterceptorId(record.getId(), record.getTenantId()).stream()
                .map(InterceptorAssembler::toTestCaseResponse)
                .toList();
    }

    /**
     * 创建测试用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterceptorTestCaseResponse createTestCase(Long interceptorId, InterceptorTestCaseSaveRequest request) {
        InterceptorRecord record = interceptorSupportManager.requireInterceptor(interceptorId);
        validateTestCaseRequest(request);
        InterceptorTestCaseRecord testCase = new InterceptorTestCaseRecord();
        InterceptorAssembler.fillTestCaseRecord(
                testCase,
                record,
                request,
                interceptorSupportManager.toJson(request.getInputPayload()),
                interceptorSupportManager.toJson(commonJsonUtils.safeMap(request.getContextPayload()))
        );
        interceptorTestCaseRecordService.save(testCase);
        return InterceptorAssembler.toTestCaseResponse(testCase);
    }

    /**
     * 运行测试用例。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterceptorDebugResponse runTestCase(Long testCaseId) {
        InterceptorTestCaseRecord testCase = interceptorTestCaseRecordService.getById(testCaseId);
        if (testCase == null || !Objects.equals(testCase.getTenantId(), interceptorSupportManager.getCurrentTenantId())) {
            throw BusinessExceptions.notFound("未找到 Interceptor 测试用例：" + testCaseId);
        }
        InterceptorDebugRequest request = new InterceptorDebugRequest();
        request.setInterceptorId(testCase.getInterceptorId());
        request.setRequestPayloadJson(testCase.getInputPayloadJson());
        request.setContextPayload(interceptorSupportManager.parseMap(testCase.getContextPayloadJson()));
        request.setSourceType(InterceptorManagementConstants.SOURCE_TYPE_TEST);
        InterceptorDebugResponse response = debugInterceptor(request);
        testCase.setLastRunStatus(response.getExecuteStatus());
        testCase.setLastRunDurationMs(response.getElapsedMs());
        testCase.setLastRunAt(LocalDateTime.now());
        testCase.setLastResultJson(response.getResponsePayloadJson());
        interceptorTestCaseRecordService.updateById(testCase);
        return response;
    }

    /**
     * 查询绑定列表。
     */
    public List<InterceptorBindingResponse> listBindings(Long interceptorId) {
        InterceptorRecord record = interceptorSupportManager.requireInterceptor(interceptorId);
        return interceptorAgentBindingRecordService.listByInterceptorId(record.getId(), record.getTenantId()).stream()
                .map(InterceptorAssembler::toBindingResponse)
                .toList();
    }

    /**
     * 创建绑定。
     */
    @Transactional(rollbackFor = Exception.class)
    public InterceptorBindingResponse createBinding(Long interceptorId, InterceptorBindingSaveRequest request) {
        InterceptorRecord record = interceptorSupportManager.requireInterceptor(interceptorId);
        validateBindingRequest(request);
        InterceptorAgentBindingRecord binding = new InterceptorAgentBindingRecord();
        InterceptorAssembler.fillBindingRecord(binding, record, request);
        interceptorAgentBindingRecordService.save(binding);
        return InterceptorAssembler.toBindingResponse(binding);
    }

    private InterceptorResponse toResponse(InterceptorRecord record) {
        return InterceptorAssembler.toResponse(
                record,
                interceptorSupportManager.parseSnapshot(record.getExt()),
                interceptorSupportManager.countBindings(record.getId()),
                interceptorSupportManager.countTestCases(record.getId()),
                interceptorSupportManager.countLogs(record.getId()),
                interceptorVersionRecordService.listByInterceptorId(record.getId(), record.getTenantId())
        );
    }

    private void createVersion(InterceptorRecord record, Integer versionNo, String versionStatus, String publishStatus) {
        InterceptorSnapshotDTO snapshot = interceptorSupportManager.parseSnapshot(record.getExt());
        InterceptorVersionRecord version = new InterceptorVersionRecord();
        version.setInterceptorId(record.getId());
        version.setInterceptorCode(record.getInterceptorCode());
        version.setInterceptorName(record.getInterceptorName());
        version.setTenantId(record.getTenantId());
        version.setVersionNo(versionNo);
        version.setVersionCode(snapshot.getVersionCode());
        version.setVersionDescription(snapshot.getVersionDescription());
        version.setVersionStatus(versionStatus);
        version.setPublishStatus(publishStatus);
        version.setSnapshotJson(record.getExt());
        interceptorVersionRecordService.save(version);
    }

    private void markCurrentVersionAsHistory(InterceptorRecord record) {
        interceptorVersionRecordService.update(Wrappers.lambdaUpdate(InterceptorVersionRecord.class)
                .eq(InterceptorVersionRecord::getInterceptorId, record.getId())
                .eq(InterceptorVersionRecord::getTenantId, record.getTenantId())
                .eq(InterceptorVersionRecord::getVersionStatus, InterceptorManagementConstants.VERSION_STATUS_CURRENT)
                .set(InterceptorVersionRecord::getVersionStatus, InterceptorManagementConstants.VERSION_STATUS_HISTORY));
    }

    private void updateCurrentVersionPublishStatus(InterceptorRecord record, String publishStatus) {
        interceptorVersionRecordService.update(Wrappers.lambdaUpdate(InterceptorVersionRecord.class)
                .eq(InterceptorVersionRecord::getInterceptorId, record.getId())
                .eq(InterceptorVersionRecord::getTenantId, record.getTenantId())
                .eq(InterceptorVersionRecord::getVersionNo, record.getCurrentVersionNo())
                .set(InterceptorVersionRecord::getPublishStatus, publishStatus));
    }

    private void updateSnapshotPublishStatus(InterceptorRecord record, String publishStatus) {
        InterceptorSnapshotDTO snapshot = interceptorSupportManager.parseSnapshot(record.getExt());
        snapshot.setPublishStatus(publishStatus);
        snapshot.setInterceptorStatus(record.getInterceptorStatus());
        record.setExt(interceptorSupportManager.toJson(snapshot));
        interceptorRecordService.updateById(record);
    }

    private InterceptorSnapshotDTO toSnapshot(InterceptorSaveRequest request, String publishStatus) {
        return InterceptorSnapshotDTO.builder()
                .interceptorCode(CommonTextUtils.trim(request.getInterceptorCode()))
                .interceptorName(CommonTextUtils.trim(request.getInterceptorName()))
                .description(CommonTextUtils.trimToNull(request.getDescription()))
                .interceptorType(CommonTextUtils.trim(request.getInterceptorType()))
                .interceptorStage(CommonTextUtils.trim(request.getInterceptorStage()))
                .interceptorStatus(CommonTextUtils.trim(request.getInterceptorStatus()))
                .publishStatus(publishStatus)
                .riskLevel(CommonTextUtils.trim(request.getRiskLevel()))
                .triggerMode(CommonTextUtils.trim(request.getTriggerMode()))
                .failStrategy(CommonTextUtils.trim(request.getFailStrategy()))
                .sortWeight(request.getSortWeight())
                .timeoutMs(request.getTimeoutMs())
                .hotUpdateEnabled(request.getHotUpdateEnabled())
                .versionCode(CommonTextUtils.trimToNull(request.getVersionCode()))
                .versionDescription(CommonTextUtils.trimToNull(request.getVersionDescription()))
                .builtinInterceptorKey(CommonTextUtils.trimToNull(request.getBuiltinInterceptorKey()))
                .scriptLanguage(CommonTextUtils.trimToNull(request.getScriptLanguage()))
                .tags(CommonTextUtils.emptyIfNull(request.getTags()))
                .targetChannels(CommonTextUtils.emptyIfNull(request.getTargetChannels()))
                .targetEnvironments(CommonTextUtils.emptyIfNull(request.getTargetEnvironments()))
                .targetAgentCodes(CommonTextUtils.emptyIfNull(request.getTargetAgentCodes()))
                .targetModelCodes(CommonTextUtils.emptyIfNull(request.getTargetModelCodes()))
                .conditionConfig(commonJsonUtils.safeMap(request.getConditionConfig()))
                .runtimeConfig(commonJsonUtils.safeMap(request.getRuntimeConfig()))
                .securityConfig(commonJsonUtils.safeMap(request.getSecurityConfig()))
                .observabilityConfig(commonJsonUtils.safeMap(request.getObservabilityConfig()))
                .degradationConfig(commonJsonUtils.safeMap(request.getDegradationConfig()))
                .interceptorConfig(commonJsonUtils.safeMap(request.getInterceptorConfig()))
                .scriptContent(CommonTextUtils.trimToNull(request.getScriptContent()))
                .testPayloadJson(CommonTextUtils.trimToNull(request.getTestPayloadJson()))
                .build();
    }

    private SimulatedResult simulateInterceptor(
            InterceptorRecord record,
            InterceptorSnapshotDTO snapshot,
            Map<String, Object> requestPayload,
            Map<String, Object> contextPayload
    ) {
        String key = StringUtils.hasText(record.getBuiltinInterceptorKey())
                ? record.getBuiltinInterceptorKey()
                : record.getInterceptorType();
        Map<String, Object> config = commonJsonUtils.safeMap(snapshot.getInterceptorConfig());
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("interceptorStage", record.getInterceptorStage());
        trace.put("builtinInterceptorKey", key);
        trace.put("hotUpdateEnabled", record.getHotUpdateEnabled());
        trace.put("conditionMatched", true);

        Map<String, Object> responsePayload = switch (key) {
            case "TOOL_RETRY" -> simulateToolRetry(config, requestPayload, trace);
            case "TODO_LIST" -> simulateTodoList(config, requestPayload, trace);
            case "TOOL_SELECTION" -> simulateToolSelection(config, requestPayload, contextPayload, trace);
            case "TOOL_EMULATOR" -> simulateToolEmulator(config, requestPayload, trace);
            case "CONTEXT_EDITING" -> simulateContextEditing(config, requestPayload, trace);
            default -> simulateGeneric(config, requestPayload, contextPayload, trace);
        };

        boolean blocked = Boolean.TRUE.equals(responsePayload.get("blocked"));
        if (blocked && "BLOCK".equals(record.getFailStrategy())) {
            return new SimulatedResult(0, InterceptorManagementConstants.EXECUTE_STATUS_FAILED, "Interceptor blocked by policy", responsePayload, trace);
        }
        return new SimulatedResult(1, InterceptorManagementConstants.EXECUTE_STATUS_SUCCESS, null, responsePayload, trace);
    }

    private Map<String, Object> simulateToolRetry(Map<String, Object> config, Map<String, Object> requestPayload, Map<String, Object> trace) {
        boolean failure = "FAILED".equalsIgnoreCase(String.valueOf(requestPayload.getOrDefault("toolStatus", "SUCCESS")));
        int maxRetries = interceptorSupportManager.numberValue(config.get("maxRetries"), 2);
        trace.put("retryPlan", Map.of("maxRetries", maxRetries, "backoffFactor", config.getOrDefault("backoffFactor", 1.0)));
        return interceptorSupportManager.orderedMap(
                "action", failure ? "RETRY" : "PASS",
                "retryCount", failure ? maxRetries : 0,
                "toolName", requestPayload.get("toolName"),
                "blocked", false
        );
    }

    private Map<String, Object> simulateTodoList(Map<String, Object> config, Map<String, Object> requestPayload, Map<String, Object> trace) {
        String input = String.valueOf(requestPayload.getOrDefault("input", ""));
        List<String> items = new ArrayList<>();
        for (String item : input.split("[，,、;；\\n]")) {
            String normalized = item.trim();
            if (StringUtils.hasText(normalized)) {
                items.add(normalized);
            }
        }
        trace.put("toolDescription", config.get("toolDescription"));
        return interceptorSupportManager.orderedMap("action", "CREATE_TODO_LIST", "todoItems", items, "blocked", false);
    }

    private Map<String, Object> simulateToolSelection(
            Map<String, Object> config,
            Map<String, Object> requestPayload,
            Map<String, Object> contextPayload,
            Map<String, Object> trace
    ) {
        List<String> candidates = CommonTextUtils.stringList(
                interceptorSupportManager.firstNonNull(requestPayload.get("toolCandidates"), contextPayload.get("toolCandidates"))
        );
        List<String> alwaysInclude = CommonTextUtils.stringList(config.get("alwaysInclude"));
        int maxTools = interceptorSupportManager.numberValue(config.get("maxTools"), 3);
        List<String> selected = new ArrayList<>();
        alwaysInclude.forEach(item -> interceptorSupportManager.addIfAbsent(selected, item));
        candidates.stream().limit(Math.max(0, maxTools - selected.size())).forEach(item -> interceptorSupportManager.addIfAbsent(selected, item));
        boolean blocked = selected.isEmpty() && "BLOCK".equals(config.getOrDefault("emptySelectionStrategy", "BLOCK"));
        trace.put("candidateCount", candidates.size());
        return interceptorSupportManager.orderedMap("action", "SELECT_TOOLS", "selectedTools", selected, "blocked", blocked);
    }

    private Map<String, Object> simulateToolEmulator(Map<String, Object> config, Map<String, Object> requestPayload, Map<String, Object> trace) {
        Map<String, Object> mockResponses = commonJsonUtils.objectMap(config.get("mockResponses"));
        List<Map<String, Object>> responses = new ArrayList<>();
        for (Object item : interceptorSupportManager.objectList(requestPayload.get("toolCalls"))) {
            Map<String, Object> call = commonJsonUtils.objectMap(item);
            String toolName = String.valueOf(call.getOrDefault("name", ""));
            responses.add(interceptorSupportManager.orderedMap(
                    "toolName", toolName,
                    "response", mockResponses.getOrDefault(toolName, Map.of("mocked", true))
            ));
        }
        trace.put("emulateAll", config.getOrDefault("emulateAll", false));
        return interceptorSupportManager.orderedMap("action", "EMULATE_TOOLS", "toolResponses", responses, "blocked", false);
    }

    private Map<String, Object> simulateContextEditing(Map<String, Object> config, Map<String, Object> requestPayload, Map<String, Object> trace) {
        List<Object> messages = interceptorSupportManager.objectList(requestPayload.get("messages"));
        int keep = interceptorSupportManager.numberValue(config.get("keep"), 6);
        List<Object> keptMessages = messages.size() <= keep ? messages : messages.subList(messages.size() - keep, messages.size());
        trace.put("originalMessageCount", messages.size());
        trace.put("keptMessageCount", keptMessages.size());
        return interceptorSupportManager.orderedMap("action", "EDIT_CONTEXT", "messages", keptMessages, "blocked", false);
    }

    private Map<String, Object> simulateGeneric(
            Map<String, Object> config,
            Map<String, Object> requestPayload,
            Map<String, Object> contextPayload,
        Map<String, Object> trace
    ) {
        trace.put("configKeys", config.keySet());
        return interceptorSupportManager.orderedMap(
                "action", "PASS_THROUGH",
                "requestPayload", requestPayload,
                "contextPayload", contextPayload,
                "blocked", false
        );
    }

    private void saveExecutionLog(
            InterceptorRecord record,
            InterceptorDebugRequest request,
            String sourceType,
            Long sourceId,
            InterceptorDebugResponse response
    ) {
        InterceptorExecutionLogRecord log = new InterceptorExecutionLogRecord();
        log.setInterceptorId(record.getId());
        log.setInterceptorCode(record.getInterceptorCode());
        log.setInterceptorName(record.getInterceptorName());
        log.setTenantId(record.getTenantId());
        log.setSourceType(sourceType);
        log.setSourceId(sourceId);
        log.setTraceId(UUID.randomUUID().toString());
        log.setAgentCode(request == null ? null : request.getAgentCode());
        log.setSessionCode(request == null ? null : request.getSessionCode());
        log.setRequestPayloadJson(response.getRequestPayloadJson());
        log.setContextPayloadJson(request == null ? null : interceptorSupportManager.toJson(commonJsonUtils.safeMap(request.getContextPayload())));
        log.setResponsePayloadJson(response.getResponsePayloadJson());
        log.setExecuteStatus(response.getExecuteStatus());
        log.setSuccessFlag(response.getSuccessFlag());
        log.setElapsedMs(response.getElapsedMs());
        log.setFailureReason(response.getFailureReason());
        log.setOperatorUserId(interceptorSupportManager.getCurrentUserId());
        log.setOperatorUserName(interceptorSupportManager.getCurrentUserName());
        interceptorExecutionLogRecordService.save(log);
    }

    private InterceptorRecord requireDeletedInterceptor(Long interceptorId) {
        InterceptorRecord record = interceptorRecordService.getById(interceptorId);
        if (record == null || !Objects.equals(record.getTenantId(), interceptorSupportManager.getCurrentTenantId())
                || !Integer.valueOf(1).equals(record.getDeletedFlag())) {
            throw BusinessExceptions.notFound("未找到已删除 Interceptor：" + interceptorId);
        }
        return record;
    }

    private void validateSaveRequest(InterceptorSaveRequest request, boolean create) {
        if (request == null) {
            throw BusinessExceptions.badRequest("Interceptor 配置不能为空");
        }
        if (!StringUtils.hasText(request.getInterceptorCode())) {
            throw BusinessExceptions.badRequest("Interceptor 编码不能为空");
        }
        if (!StringUtils.hasText(request.getInterceptorName())) {
            throw BusinessExceptions.badRequest("Interceptor 名称不能为空");
        }
        if (!StringUtils.hasText(request.getInterceptorType())) {
            throw BusinessExceptions.badRequest("Interceptor 类型不能为空");
        }
        if (!StringUtils.hasText(request.getInterceptorStage())) {
            throw BusinessExceptions.badRequest("Interceptor 阶段不能为空");
        }
        if (request.getTimeoutMs() != null && request.getTimeoutMs() < 100) {
            throw BusinessExceptions.badRequest("超时时间不能小于 100ms");
        }
        if (create && request.getInterceptorCode().length() > 128) {
            throw BusinessExceptions.badRequest("Interceptor 编码长度不能超过 128");
        }
    }

    private void validateTestCaseRequest(InterceptorTestCaseSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getCaseName())) {
            throw BusinessExceptions.badRequest("测试用例名称不能为空");
        }
        if (request.getInputPayload() == null) {
            throw BusinessExceptions.badRequest("测试输入不能为空");
        }
    }

    private void validateBindingRequest(InterceptorBindingSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getBindingName())) {
            throw BusinessExceptions.badRequest("绑定名称不能为空");
        }
        if (!StringUtils.hasText(request.getBindingScope())) {
            throw BusinessExceptions.badRequest("绑定范围不能为空");
        }
    }

    private record SimulatedResult(
            Integer successFlag,
            String executeStatus,
            String failureReason,
            Map<String, Object> responsePayload,
            Map<String, Object> tracePayload
    ) {
    }
}
