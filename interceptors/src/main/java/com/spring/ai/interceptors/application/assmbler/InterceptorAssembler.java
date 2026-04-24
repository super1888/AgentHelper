package com.spring.ai.interceptors.application.assmbler;

import com.spring.ai.common.repository.enitiy.InterceptorAgentBindingRecord;
import com.spring.ai.common.repository.enitiy.InterceptorExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.InterceptorRecord;
import com.spring.ai.common.repository.enitiy.InterceptorTestCaseRecord;
import com.spring.ai.common.repository.enitiy.InterceptorVersionRecord;
import com.spring.ai.common.utils.CommonTextUtils;
import com.spring.ai.interceptors.domain.dto.InterceptorCatalogDTO;
import com.spring.ai.interceptors.domain.dto.InterceptorSnapshotDTO;
import com.spring.ai.interceptors.domain.request.InterceptorBindingSaveRequest;
import com.spring.ai.interceptors.domain.request.InterceptorSaveRequest;
import com.spring.ai.interceptors.domain.request.InterceptorTestCaseSaveRequest;
import com.spring.ai.interceptors.domain.response.InterceptorBindingResponse;
import com.spring.ai.interceptors.domain.response.InterceptorCatalogResponse;
import com.spring.ai.interceptors.domain.response.InterceptorDebugResponse;
import com.spring.ai.interceptors.domain.response.InterceptorExecutionLogResponse;
import com.spring.ai.interceptors.domain.response.InterceptorResponse;
import com.spring.ai.interceptors.domain.response.InterceptorTestCaseResponse;
import com.spring.ai.interceptors.domain.response.InterceptorVersionResponse;
import java.util.List;
import java.util.Map;

/**
 * 文件用途：Interceptor 对象组装器
 * 核心职责：统一处理拦截器实体与接口对象的转换
 */
public final class InterceptorAssembler {

    private InterceptorAssembler() {
    }

    public static InterceptorResponse toResponse(
            InterceptorRecord record,
            InterceptorSnapshotDTO snapshot,
            Integer bindingCount,
            Integer testCaseCount,
            Integer logCount,
            List<InterceptorVersionRecord> versions
    ) {
        return InterceptorResponse.builder()
                .id(record.getId())
                .interceptorCode(record.getInterceptorCode())
                .interceptorName(record.getInterceptorName())
                .description(record.getDescription())
                .interceptorType(record.getInterceptorType())
                .interceptorStage(record.getInterceptorStage())
                .interceptorStatus(record.getInterceptorStatus())
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
                .builtinInterceptorKey(record.getBuiltinInterceptorKey())
                .scriptLanguage(record.getScriptLanguage())
                .tags(snapshot == null || snapshot.getTags() == null ? List.of() : snapshot.getTags())
                .targetChannels(snapshot == null || snapshot.getTargetChannels() == null ? List.of() : snapshot.getTargetChannels())
                .targetEnvironments(snapshot == null || snapshot.getTargetEnvironments() == null ? List.of() : snapshot.getTargetEnvironments())
                .targetAgentCodes(snapshot == null || snapshot.getTargetAgentCodes() == null ? List.of() : snapshot.getTargetAgentCodes())
                .targetModelCodes(snapshot == null || snapshot.getTargetModelCodes() == null ? List.of() : snapshot.getTargetModelCodes())
                .conditionConfig(snapshot == null ? null : snapshot.getConditionConfig())
                .runtimeConfig(snapshot == null ? null : snapshot.getRuntimeConfig())
                .securityConfig(snapshot == null ? null : snapshot.getSecurityConfig())
                .observabilityConfig(snapshot == null ? null : snapshot.getObservabilityConfig())
                .degradationConfig(snapshot == null ? null : snapshot.getDegradationConfig())
                .interceptorConfig(snapshot == null ? null : snapshot.getInterceptorConfig())
                .scriptContent(snapshot == null ? null : snapshot.getScriptContent())
                .testPayloadJson(snapshot == null ? null : snapshot.getTestPayloadJson())
                .tenantId(record.getTenantId())
                .ownerUserId(record.getOwnerUserId())
                .ownerUserName(record.getOwnerUserName())
                .bindingCount(bindingCount)
                .testCaseCount(testCaseCount)
                .logCount(logCount)
                .remark(record.getRemark())
                .createTime(CommonTextUtils.toEpochMilli(record.getCreateTime()))
                .updateTime(CommonTextUtils.toEpochMilli(record.getUpdateTime()))
                .versions(versions == null ? List.of() : versions.stream().map(InterceptorAssembler::toVersionResponse).toList())
                .build();
    }

    public static InterceptorVersionResponse toVersionResponse(InterceptorVersionRecord record) {
        return InterceptorVersionResponse.builder()
                .id(record.getId())
                .versionNo(record.getVersionNo())
                .versionCode(record.getVersionCode())
                .versionDescription(record.getVersionDescription())
                .versionStatus(record.getVersionStatus())
                .publishStatus(record.getPublishStatus())
                .snapshotJson(record.getSnapshotJson())
                .createTime(CommonTextUtils.toEpochMilli(record.getCreateTime()))
                .build();
    }

    public static InterceptorExecutionLogResponse toLogResponse(InterceptorExecutionLogRecord record) {
        return InterceptorExecutionLogResponse.builder()
                .id(record.getId())
                .interceptorId(record.getInterceptorId())
                .interceptorCode(record.getInterceptorCode())
                .interceptorName(record.getInterceptorName())
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
                .createTime(CommonTextUtils.toEpochMilli(record.getCreateTime()))
                .build();
    }

    public static InterceptorTestCaseResponse toTestCaseResponse(InterceptorTestCaseRecord record) {
        return InterceptorTestCaseResponse.builder()
                .id(record.getId())
                .interceptorId(record.getInterceptorId())
                .interceptorCode(record.getInterceptorCode())
                .caseName(record.getCaseName())
                .inputPayloadJson(record.getInputPayloadJson())
                .contextPayloadJson(record.getContextPayloadJson())
                .expectedSuccess(record.getExpectedSuccess())
                .expectedResponseContains(record.getExpectedResponseContains())
                .enabled(record.getEnabled())
                .lastRunStatus(record.getLastRunStatus())
                .lastRunDurationMs(record.getLastRunDurationMs())
                .lastRunAt(CommonTextUtils.toEpochMilli(record.getLastRunAt()))
                .lastResultJson(record.getLastResultJson())
                .build();
    }

    public static InterceptorBindingResponse toBindingResponse(InterceptorAgentBindingRecord record) {
        return InterceptorBindingResponse.builder()
                .id(record.getId())
                .interceptorId(record.getInterceptorId())
                .interceptorCode(record.getInterceptorCode())
                .bindingName(record.getBindingName())
                .bindingScope(record.getBindingScope())
                .targetAgentCode(record.getTargetAgentCode())
                .targetModelCode(record.getTargetModelCode())
                .environmentCode(record.getEnvironmentCode())
                .priorityNo(record.getPriorityNo())
                .enabled(record.getEnabled())
                .remark(record.getRemark())
                .createTime(CommonTextUtils.toEpochMilli(record.getCreateTime()))
                .build();
    }

    public static InterceptorCatalogResponse toCatalogResponse(InterceptorCatalogDTO dto) {
        return InterceptorCatalogResponse.builder()
                .interceptorKey(dto.getInterceptorKey())
                .interceptorName(dto.getInterceptorName())
                .description(dto.getDescription())
                .interceptorType(dto.getInterceptorType())
                .interceptorStage(dto.getInterceptorStage())
                .riskLevel(dto.getRiskLevel())
                .failStrategy(dto.getFailStrategy())
                .defaultConfigJson(dto.getDefaultConfigJson())
                .defaultTestPayloadJson(dto.getDefaultTestPayloadJson())
                .tags(dto.getTags())
                .build();
    }

    public static InterceptorDebugResponse toDebugResponse(
            InterceptorRecord record,
            Integer successFlag,
            String executeStatus,
            String responsePayloadJson,
            String failureReason,
            Long elapsedMs,
            String requestPayloadJson,
            Map<String, Object> tracePayload
    ) {
        return InterceptorDebugResponse.builder()
                .interceptorId(record.getId())
                .interceptorCode(record.getInterceptorCode())
                .interceptorName(record.getInterceptorName())
                .successFlag(successFlag)
                .executeStatus(executeStatus)
                .responseText(Integer.valueOf(1).equals(successFlag) ? "Interceptor 调试执行成功" : "Interceptor 调试执行失败")
                .failureReason(failureReason)
                .elapsedMs(elapsedMs)
                .requestPayloadJson(requestPayloadJson)
                .responsePayloadJson(responsePayloadJson)
                .tracePayload(tracePayload)
                .build();
    }

    public static void mergeRecord(InterceptorRecord record, InterceptorSaveRequest request) {
        record.setInterceptorCode(CommonTextUtils.trim(request.getInterceptorCode()));
        record.setInterceptorName(CommonTextUtils.trim(request.getInterceptorName()));
        record.setDescription(CommonTextUtils.trimToNull(request.getDescription()));
        record.setInterceptorType(CommonTextUtils.trim(request.getInterceptorType()));
        record.setInterceptorStage(CommonTextUtils.trim(request.getInterceptorStage()));
        record.setInterceptorStatus(CommonTextUtils.trim(request.getInterceptorStatus()));
        record.setRiskLevel(CommonTextUtils.trim(request.getRiskLevel()));
        record.setTriggerMode(CommonTextUtils.trim(request.getTriggerMode()));
        record.setFailStrategy(CommonTextUtils.trim(request.getFailStrategy()));
        record.setSortWeight(request.getSortWeight());
        record.setTimeoutMs(request.getTimeoutMs());
        record.setHotUpdateEnabled(request.getHotUpdateEnabled());
        record.setBuiltinInterceptorKey(CommonTextUtils.trimToNull(request.getBuiltinInterceptorKey()));
        record.setScriptLanguage(CommonTextUtils.trimToNull(request.getScriptLanguage()));
        record.setRemark(CommonTextUtils.trimToNull(request.getRemark()));
    }

    public static void fillTestCaseRecord(
            InterceptorTestCaseRecord record,
            InterceptorRecord interceptor,
            InterceptorTestCaseSaveRequest request,
            String inputPayloadJson,
            String contextPayloadJson
    ) {
        record.setInterceptorId(interceptor.getId());
        record.setInterceptorCode(interceptor.getInterceptorCode());
        record.setCaseName(CommonTextUtils.trim(request.getCaseName()));
        record.setInputPayloadJson(inputPayloadJson);
        record.setContextPayloadJson(contextPayloadJson);
        record.setExpectedSuccess(request.getExpectedSuccess());
        record.setExpectedResponseContains(CommonTextUtils.trimToNull(request.getExpectedResponseContains()));
        record.setEnabled(request.getEnabled());
        record.setTenantId(interceptor.getTenantId());
    }

    public static void fillBindingRecord(InterceptorAgentBindingRecord record, InterceptorRecord interceptor, InterceptorBindingSaveRequest request) {
        record.setInterceptorId(interceptor.getId());
        record.setInterceptorCode(interceptor.getInterceptorCode());
        record.setBindingName(CommonTextUtils.trim(request.getBindingName()));
        record.setBindingScope(CommonTextUtils.trim(request.getBindingScope()));
        record.setTargetAgentCode(CommonTextUtils.trimToNull(request.getTargetAgentCode()));
        record.setTargetModelCode(CommonTextUtils.trimToNull(request.getTargetModelCode()));
        record.setEnvironmentCode(CommonTextUtils.trimToNull(request.getEnvironmentCode()));
        record.setPriorityNo(request.getPriorityNo());
        record.setEnabled(request.getEnabled());
        record.setRemark(CommonTextUtils.trimToNull(request.getRemark()));
        record.setTenantId(interceptor.getTenantId());
    }

}
