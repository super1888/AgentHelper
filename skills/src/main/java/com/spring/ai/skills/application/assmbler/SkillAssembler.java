package com.spring.ai.skills.application.assmbler;

import com.spring.ai.common.repository.enitiy.SkillRecord;
import com.spring.ai.common.repository.enitiy.SkillVersionRecord;
import com.spring.ai.skills.domain.dto.SkillSnapshotDTO;
import com.spring.ai.skills.domain.response.SkillResponse;
import com.spring.ai.skills.domain.response.SkillVersionResponse;
import java.time.ZoneId;
import java.util.List;

public final class SkillAssembler {

    private SkillAssembler() {
    }

    public static SkillResponse toResponse(SkillRecord record, SkillSnapshotDTO snapshot, List<SkillVersionRecord> versions) {
        return SkillResponse.builder()
                .id(record.getId())
                .skillCode(record.getSkillCode())
                .skillName(record.getSkillName())
                .description(record.getDescription())
                .skillCategory(record.getSkillCategory())
                .skillStatus(record.getSkillStatus())
                .publishStatus(record.getPublishStatus())
                .versionMode(record.getVersionMode())
                .currentVersionNo(record.getCurrentVersionNo())
                .latestVersionNo(record.getLatestVersionNo())
                .publishedVersionNo(record.getPublishedVersionNo())
                .hotUpdateEnabled(record.getHotUpdateEnabled())
                .tenantId(record.getTenantId())
                .ownerUserId(record.getOwnerUserId())
                .ownerUserName(record.getOwnerUserName())
                .intentConfigs(snapshot.getIntentConfigs())
                .executionConfig(snapshot.getExecutionConfig())
                .routingConfig(snapshot.getRoutingConfig())
                .permissionConfig(snapshot.getPermissionConfig())
                .observabilityConfig(snapshot.getObservabilityConfig())
                .releaseConfig(snapshot.getReleaseConfig())
                .batchConfig(snapshot.getBatchConfig())
                .workflowConfig(snapshot.getWorkflowConfig())
                .versions(versions == null ? List.of() : versions.stream().map(SkillAssembler::toVersionResponse).toList())
                .remark(snapshot.getRemark())
                .createTime(toEpochMilli(record.getCreateTime()))
                .updateTime(toEpochMilli(record.getUpdateTime()))
                .build();
    }

    public static SkillVersionResponse toVersionResponse(SkillVersionRecord versionRecord) {
        return SkillVersionResponse.builder()
                .id(versionRecord.getId())
                .versionNo(versionRecord.getVersionNo())
                .versionStatus(versionRecord.getVersionStatus())
                .publishStatus(versionRecord.getPublishStatus())
                .createTime(toEpochMilli(versionRecord.getCreateTime()))
                .build();
    }

    public static Long toEpochMilli(java.time.LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
