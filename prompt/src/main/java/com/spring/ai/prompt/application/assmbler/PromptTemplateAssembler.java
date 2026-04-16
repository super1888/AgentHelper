package com.spring.ai.prompt.application.assmbler;

import com.spring.ai.common.repository.enitiy.PromptTemplateRecord;
import com.spring.ai.prompt.config.PromptTemplateConstants;
import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import com.spring.ai.prompt.domain.response.PromptTemplateResponse;
import com.spring.ai.prompt.domain.response.PromptTemplateStatisticsResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * 提示词模板对象组装器。
 */
public final class PromptTemplateAssembler {

    private PromptTemplateAssembler() {
    }

    public static PromptTemplateRecord toCreateRecord(
            Long tenantId,
            Long ownerUserId,
            String ownerUserName,
            String templateCode,
            String templateName,
            String description,
            String sourceType,
            String templateContent,
            String sourcePath,
            String ext
    ) {
        PromptTemplateRecord record = new PromptTemplateRecord();
        record.setTemplateCode(normalizeCode(templateCode));
        record.setTemplateName(trim(templateName));
        record.setDescription(trimToNull(description));
        record.setTemplateType(PromptTemplateConstants.TEMPLATE_TYPE_SYSTEM);
        record.setSourceType(sourceType);
        record.setTemplateContent(templateContent);
        record.setSourcePath(trimToNull(sourcePath));
        record.setExt(trimToNull(ext));
        record.setTemplateStatus(PromptTemplateConstants.TEMPLATE_STATUS_ENABLED);
        record.setTenantId(tenantId);
        record.setOwnerUserId(ownerUserId);
        record.setOwnerUserName(ownerUserName);
        return record;
    }

    public static void mergeForUpdate(
            PromptTemplateRecord record,
            String templateName,
            String description,
            String sourceType,
            String templateContent,
            String sourcePath,
            String templateStatus,
            String ext
    ) {
        record.setTemplateName(trim(templateName));
        record.setDescription(trimToNull(description));
        record.setSourceType(sourceType);
        record.setTemplateContent(templateContent);
        record.setSourcePath(trimToNull(sourcePath));
        record.setExt(trimToNull(ext));
        if (StringUtils.hasText(templateStatus)) {
            record.setTemplateStatus(templateStatus);
        }
    }

    public static PromptTemplateResponse toResponse(
            PromptTemplateRecord record,
            List<PromptTemplateVariableDTO> variableDefinitions
    ) {
        return PromptTemplateResponse.builder()
                .id(record.getId())
                .templateCode(record.getTemplateCode())
                .templateName(record.getTemplateName())
                .description(record.getDescription())
                .templateType(record.getTemplateType())
                .sourceType(record.getSourceType())
                .templateContent(record.getTemplateContent())
                .sourcePath(record.getSourcePath())
                .templateStatus(record.getTemplateStatus())
                .ownerUserId(record.getOwnerUserId())
                .ownerUserName(record.getOwnerUserName())
                .variableDefinitions(variableDefinitions)
                .createTime(toEpochMilli(record.getCreateTime()))
                .updateTime(toEpochMilli(record.getUpdateTime()))
                .build();
    }

    public static PromptTemplateStatisticsResponse toStatistics(List<PromptTemplateRecord> records) {
        return PromptTemplateStatisticsResponse.builder()
                .totalCount(records.size())
                .enabledCount((int) records.stream()
                        .filter(item -> PromptTemplateConstants.TEMPLATE_STATUS_ENABLED.equals(item.getTemplateStatus()))
                        .count())
                .disabledCount((int) records.stream()
                        .filter(item -> PromptTemplateConstants.TEMPLATE_STATUS_DISABLED.equals(item.getTemplateStatus()))
                        .count())
                .inlineCount((int) records.stream()
                        .filter(item -> PromptTemplateConstants.SOURCE_TYPE_INLINE.equals(item.getSourceType()))
                        .count())
                .fileCount((int) records.stream()
                        .filter(item -> PromptTemplateConstants.SOURCE_TYPE_FILE.equals(item.getSourceType()))
                        .count())
                .build();
    }

    public static Long toEpochMilli(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static String normalizeCode(String templateCode) {
        return trim(templateCode) == null ? null : trim(templateCode).toUpperCase();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
