package com.spring.ai.skills.application.assmbler;

import com.spring.ai.common.repository.enitiy.SkillExecutionLogRecord;
import com.spring.ai.common.repository.enitiy.SkillRecord;
import com.spring.ai.common.repository.enitiy.SkillTestCaseRecord;
import com.spring.ai.common.repository.enitiy.SkillVersionRecord;
import com.spring.ai.skills.domain.dto.SkillSnapshotDTO;
import com.spring.ai.skills.domain.response.SkillExecutionLogResponse;
import com.spring.ai.skills.domain.response.SkillResponse;
import com.spring.ai.skills.domain.response.SkillTestCaseResponse;
import com.spring.ai.skills.domain.response.SkillVersionResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public final class SkillAssembler {

    private SkillAssembler() {
    }

    public static SkillResponse toResponse(
            SkillRecord record,
            SkillSnapshotDTO snapshot,
            List<SkillVersionRecord> versions,
            int testCaseCount,
            int logCount
    ) {
        return SkillResponse.builder()
                .id(record.getId())
                .skillCode(record.getSkillCode())
                .skillName(record.getSkillName())
                .description(record.getDescription())
                .skillType(record.getSkillType())
                .skillCategory(record.getSkillCategory())
                .categoryChain(snapshot.getCategoryChain())
                .tags(snapshot.getTags())
                .skillStatus(record.getSkillStatus())
                .publishStatus(record.getPublishStatus())
                .sortWeight(record.getSortWeight())
                .versionCode(snapshot.getVersionCode())
                .versionDescription(snapshot.getVersionDescription())
                .versionMode(record.getVersionMode())
                .currentVersionNo(record.getCurrentVersionNo())
                .latestVersionNo(record.getLatestVersionNo())
                .publishedVersionNo(record.getPublishedVersionNo())
                .hotUpdateEnabled(record.getHotUpdateEnabled())
                .tenantId(record.getTenantId())
                .ownerUserId(record.getOwnerUserId())
                .ownerUserName(record.getOwnerUserName())
                .observabilityConfig(snapshot.getObservabilityConfig())
                .releaseConfig(snapshot.getReleaseConfig())
                .batchConfig(snapshot.getBatchConfig())
                .workflowConfig(snapshot.getWorkflowConfig())
                .channelAdaptations(snapshot.getChannelAdaptations())
                .marketplaceConfig(snapshot.getMarketplaceConfig())
                .versions(versions == null ? List.of() : versions.stream().map(SkillAssembler::toVersionResponse).toList())
                .testCaseCount(testCaseCount)
                .logCount(logCount)
                .remark(snapshot.getRemark())
                .createTime(toEpochMilli(record.getCreateTime()))
                .updateTime(toEpochMilli(record.getUpdateTime()))
                .build();
    }

    public static SkillVersionResponse toVersionResponse(SkillVersionRecord versionRecord) {
        return SkillVersionResponse.builder()
                .id(versionRecord.getId())
                .versionNo(versionRecord.getVersionNo())
                .versionCode(versionRecord.getVersionCode())
                .versionDescription(versionRecord.getVersionDescription())
                .versionStatus(versionRecord.getVersionStatus())
                .publishStatus(versionRecord.getPublishStatus())
                .releaseStage(versionRecord.getReleaseStage())
                .createTime(toEpochMilli(versionRecord.getCreateTime()))
                .build();
    }

    public static SkillTestCaseResponse toTestCaseResponse(SkillTestCaseRecord record) {
        return SkillTestCaseResponse.builder()
                .id(record.getId())
                .skillId(record.getSkillId())
                .skillCode(record.getSkillCode())
                .caseName(record.getCaseName())
                .inputText(record.getInputText())
                .slotPayloadJson(record.getSlotPayloadJson())
                .expectedIntent(record.getExpectedIntent())
                .expectedSuccess(record.getExpectedSuccess())
                .expectedResponseContains(record.getExpectedResponseContains())
                .channelCode(record.getChannelCode())
                .locale(record.getLocale())
                .enabled(record.getEnabled())
                .lastRunStatus(record.getLastRunStatus())
                .lastRunDurationMs(record.getLastRunDurationMs())
                .lastRunAt(toEpochMilli(record.getLastRunAt()))
                .lastResultJson(record.getLastResultJson())
                .build();
    }

    public static SkillExecutionLogResponse toLogResponse(SkillExecutionLogRecord record) {
        return SkillExecutionLogResponse.builder()
                .id(record.getId())
                .skillId(record.getSkillId())
                .skillCode(record.getSkillCode())
                .skillName(record.getSkillName())
                .sourceType(record.getSourceType())
                .sourceId(record.getSourceId())
                .traceId(record.getTraceId())
                .sessionCode(record.getSessionCode())
                .channelCode(record.getChannelCode())
                .locale(record.getLocale())
                .inputText(record.getInputText())
                .matchedIntent(record.getMatchedIntent())
                .confidenceScore(record.getConfidenceScore())
                .requestPayloadJson(record.getRequestPayloadJson())
                .responsePayloadJson(record.getResponsePayloadJson())
                .tracePayloadJson(record.getTracePayloadJson())
                .executeStatus(record.getExecuteStatus())
                .successFlag(record.getSuccessFlag())
                .elapsedMs(record.getElapsedMs())
                .failureReason(record.getFailureReason())
                .satisfactionLevel(record.getSatisfactionLevel())
                .operatorUserName(record.getOperatorUserName())
                .createTime(toEpochMilli(record.getCreateTime()))
                .build();
    }

    public static Long toEpochMilli(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
