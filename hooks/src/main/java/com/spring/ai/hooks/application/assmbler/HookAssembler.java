package com.spring.ai.hooks.application.assmbler;

import com.spring.ai.common.repository.enitiy.HookAgentBindingRecord;
import com.spring.ai.common.repository.enitiy.HookExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.HookRecord;
import com.spring.ai.common.repository.enitiy.HookTestCaseRecord;
import com.spring.ai.common.repository.enitiy.HookVersionRecord;
import com.spring.ai.hooks.domain.dto.HookCatalogDTO;
import com.spring.ai.hooks.domain.dto.HookSnapshotDTO;
import com.spring.ai.hooks.domain.request.HookBindingSaveRequest;
import com.spring.ai.hooks.domain.request.HookSaveRequest;
import com.spring.ai.hooks.domain.request.HookTestCaseSaveRequest;
import com.spring.ai.hooks.domain.response.HookBindingResponse;
import com.spring.ai.hooks.domain.response.HookCatalogResponse;
import com.spring.ai.hooks.domain.response.HookDebugResponse;
import com.spring.ai.hooks.domain.response.HookExecutionLogResponse;
import com.spring.ai.hooks.domain.response.HookResponse;
import com.spring.ai.hooks.domain.response.HookTestCaseResponse;
import com.spring.ai.hooks.domain.response.HookVersionResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 文件用途：Hook 对象组装器
 * 核心职责：统一处理 Hook 实体与响应对象的转换
 */
public final class HookAssembler {

    private HookAssembler() {
    }

    public static HookResponse toResponse(
            HookRecord record,
            HookSnapshotDTO snapshot,
            Integer bindingCount,
            Integer testCaseCount,
            Integer logCount,
            List<HookVersionRecord> versions
    ) {
        return HookResponse.builder()
                .id(record.getId())
                .hookCode(record.getHookCode())
                .hookName(record.getHookName())
                .description(record.getDescription())
                .hookType(record.getHookType())
                .hookStage(record.getHookStage())
                .hookStatus(record.getHookStatus())
                .publishStatus(record.getPublishStatus())
                .riskLevel(record.getRiskLevel())
                .triggerMode(record.getTriggerMode())
                .failStrategy(record.getFailStrategy())
                .sortWeight(record.getSortWeight())
                .timeoutMs(record.getTimeoutMs())
                .hotUpdateEnabled(record.getHotUpdateEnabled())
                .currentVersionNo(record.getCurrentVersionNo())
                .latestVersionNo(record.getLatestVersionNo())
                .publishedVersionNo(record.getPublishedVersionNo())
                .versionCode(snapshot == null ? null : snapshot.getVersionCode())
                .versionDescription(snapshot == null ? null : snapshot.getVersionDescription())
                .builtinHookKey(record.getBuiltinHookKey())
                .scriptLanguage(record.getScriptLanguage())
                .tags(snapshot == null || snapshot.getTags() == null ? Collections.emptyList() : snapshot.getTags().stream().map(item -> item.getTagName()).toList())
                .targetChannels(snapshot == null || snapshot.getTargetChannels() == null ? List.of() : snapshot.getTargetChannels())
                .targetEnvironments(snapshot == null || snapshot.getTargetEnvironments() == null ? List.of() : snapshot.getTargetEnvironments())
                .targetAgentCodes(snapshot == null || snapshot.getTargetAgentCodes() == null ? List.of() : snapshot.getTargetAgentCodes())
                .targetModelCodes(snapshot == null || snapshot.getTargetModelCodes() == null ? List.of() : snapshot.getTargetModelCodes())
                .conditionConfig(snapshot == null ? null : snapshot.getConditionConfig())
                .runtimeConfig(snapshot == null ? null : snapshot.getRuntimeConfig())
                .securityConfig(snapshot == null ? null : snapshot.getSecurityConfig())
                .observabilityConfig(snapshot == null ? null : snapshot.getObservabilityConfig())
                .degradationConfig(snapshot == null ? null : snapshot.getDegradationConfig())
                .scriptContent(snapshot == null ? null : snapshot.getScriptContent())
                .testPayloadJson(snapshot == null ? null : snapshot.getTestPayloadJson())
                .tenantId(record.getTenantId())
                .ownerUserId(record.getOwnerUserId())
                .ownerUserName(record.getOwnerUserName())
                .bindingCount(bindingCount)
                .testCaseCount(testCaseCount)
                .logCount(logCount)
                .remark(record.getRemark())
                .createTime(toEpochMilli(record.getCreateTime()))
                .updateTime(toEpochMilli(record.getUpdateTime()))
                .versions(versions == null ? List.of() : versions.stream().map(HookAssembler::toVersionResponse).toList())
                .build();
    }

    public static HookVersionResponse toVersionResponse(HookVersionRecord record) {
        return HookVersionResponse.builder()
                .id(record.getId())
                .versionNo(record.getVersionNo())
                .versionCode(record.getVersionCode())
                .versionDescription(record.getVersionDescription())
                .versionStatus(record.getVersionStatus())
                .publishStatus(record.getPublishStatus())
                .snapshotJson(record.getSnapshotJson())
                .createTime(toEpochMilli(record.getCreateTime()))
                .build();
    }

    public static HookExecutionLogResponse toLogResponse(HookExecutionLogRecord record) {
        return HookExecutionLogResponse.builder()
                .id(record.getId())
                .hookId(record.getHookId())
                .hookCode(record.getHookCode())
                .hookName(record.getHookName())
                .sourceType(record.getSourceType())
                .sourceId(record.getSourceId())
                .traceId(record.getTraceId())
                .agentCode(record.getAgentCode())
                .sessionCode(record.getSessionCode())
                .requestPayloadJson(record.getRequestPayloadJson())
                .contextPayloadJson(record.getContextPayloadJson())
                .responsePayloadJson(record.getResponsePayloadJson())
                .executeStatus(record.getExecuteStatus())
                .successFlag(record.getSuccessFlag())
                .elapsedMs(record.getElapsedMs())
                .failureReason(record.getFailureReason())
                .operatorUserName(record.getOperatorUserName())
                .createTime(toEpochMilli(record.getCreateTime()))
                .build();
    }

    public static HookTestCaseResponse toTestCaseResponse(HookTestCaseRecord record) {
        return HookTestCaseResponse.builder()
                .id(record.getId())
                .hookId(record.getHookId())
                .hookCode(record.getHookCode())
                .caseName(record.getCaseName())
                .inputPayloadJson(record.getInputPayloadJson())
                .contextPayloadJson(record.getContextPayloadJson())
                .expectedSuccess(record.getExpectedSuccess())
                .expectedResponseContains(record.getExpectedResponseContains())
                .enabled(record.getEnabled())
                .lastRunStatus(record.getLastRunStatus())
                .lastRunDurationMs(record.getLastRunDurationMs())
                .lastRunAt(toEpochMilli(record.getLastRunAt()))
                .lastResultJson(record.getLastResultJson())
                .build();
    }

    public static HookBindingResponse toBindingResponse(HookAgentBindingRecord record) {
        return HookBindingResponse.builder()
                .id(record.getId())
                .hookId(record.getHookId())
                .hookCode(record.getHookCode())
                .bindingName(record.getBindingName())
                .bindingScope(record.getBindingScope())
                .targetAgentCode(record.getTargetAgentCode())
                .targetModelCode(record.getTargetModelCode())
                .environmentCode(record.getEnvironmentCode())
                .priorityNo(record.getPriorityNo())
                .enabled(record.getEnabled())
                .remark(record.getRemark())
                .createTime(toEpochMilli(record.getCreateTime()))
                .build();
    }

    public static HookCatalogResponse toCatalogResponse(HookCatalogDTO dto) {
        return HookCatalogResponse.builder()
                .hookKey(dto.getHookKey())
                .hookName(dto.getHookName())
                .description(dto.getDescription())
                .hookType(dto.getHookType())
                .hookStage(dto.getHookStage())
                .riskLevel(dto.getRiskLevel())
                .failStrategy(dto.getFailStrategy())
                .defaultConfigJson(dto.getDefaultConfigJson())
                .defaultTestPayloadJson(dto.getDefaultTestPayloadJson())
                .tags(dto.getTags())
                .build();
    }

    public static HookDebugResponse toDebugResponse(
            HookRecord record,
            Integer successFlag,
            String executeStatus,
            String responsePayloadJson,
            String failureReason,
            Long elapsedMs,
            String requestPayloadJson,
            Map<String, Object> tracePayload
    ) {
        return HookDebugResponse.builder()
                .hookId(record.getId())
                .hookCode(record.getHookCode())
                .hookName(record.getHookName())
                .successFlag(successFlag)
                .executeStatus(executeStatus)
                .responseText(Integer.valueOf(1).equals(successFlag) ? "Hook 调试执行成功" : "Hook 调试执行失败")
                .failureReason(failureReason)
                .elapsedMs(elapsedMs)
                .requestPayloadJson(requestPayloadJson)
                .responsePayloadJson(responsePayloadJson)
                .tracePayload(tracePayload)
                .build();
    }

    public static void mergeRecord(HookRecord record, HookSaveRequest request) {
        record.setHookCode(request.getHookCode());
        record.setHookName(request.getHookName());
        record.setDescription(request.getDescription());
        record.setHookType(request.getHookType());
        record.setHookStage(request.getHookStage());
        record.setHookStatus(request.getHookStatus());
        record.setRiskLevel(request.getRiskLevel());
        record.setTriggerMode(request.getTriggerMode());
        record.setFailStrategy(request.getFailStrategy());
        record.setSortWeight(request.getSortWeight());
        record.setTimeoutMs(request.getTimeoutMs());
        record.setHotUpdateEnabled(request.getHotUpdateEnabled());
        record.setBuiltinHookKey(request.getBuiltinHookKey());
        record.setScriptLanguage(request.getScriptLanguage());
        record.setRemark(request.getRemark());
    }

    public static void fillTestCaseRecord(HookTestCaseRecord record, HookRecord hook, HookTestCaseSaveRequest request, String inputPayloadJson, String contextPayloadJson) {
        record.setHookId(hook.getId());
        record.setHookCode(hook.getHookCode());
        record.setCaseName(request.getCaseName());
        record.setInputPayloadJson(inputPayloadJson);
        record.setContextPayloadJson(contextPayloadJson);
        record.setExpectedSuccess(request.getExpectedSuccess());
        record.setExpectedResponseContains(request.getExpectedResponseContains());
        record.setEnabled(request.getEnabled());
        record.setTenantId(hook.getTenantId());
    }

    public static void fillBindingRecord(HookAgentBindingRecord record, HookRecord hook, HookBindingSaveRequest request) {
        record.setHookId(hook.getId());
        record.setHookCode(hook.getHookCode());
        record.setBindingName(request.getBindingName());
        record.setBindingScope(request.getBindingScope());
        record.setTargetAgentCode(request.getTargetAgentCode());
        record.setTargetModelCode(request.getTargetModelCode());
        record.setEnvironmentCode(request.getEnvironmentCode());
        record.setPriorityNo(request.getPriorityNo());
        record.setEnabled(request.getEnabled());
        record.setRemark(request.getRemark());
        record.setTenantId(hook.getTenantId());
    }

    private static Long toEpochMilli(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
