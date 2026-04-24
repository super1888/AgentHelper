package com.spring.ai.prompt.application.assmbler;

import com.spring.ai.common.repository.enitiy.PromptTemplateRecord;
import com.spring.ai.common.utils.CommonTextUtils;
import com.spring.ai.prompt.config.PromptTemplateConstants;
import com.spring.ai.prompt.domain.dto.PromptTemplateEnterpriseConfigDTO;
import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import com.spring.ai.prompt.domain.response.PromptTemplateResponse;
import com.spring.ai.prompt.domain.response.PromptTemplateStatisticsResponse;
import java.util.List;

/**
 * 提示词模板对象组装器。
 */
public final class PromptTemplateAssembler {

    private PromptTemplateAssembler() {
    }

    /**
     * 构建新增模板时使用的持久化记录。
     */
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
        record.setTemplateName(CommonTextUtils.trim(templateName));
        record.setDescription(CommonTextUtils.trimToNull(description));
        record.setTemplateType(PromptTemplateConstants.TEMPLATE_TYPE_SYSTEM);
        record.setSourceType(sourceType);
        record.setTemplateContent(templateContent);
        record.setSourcePath(CommonTextUtils.trimToNull(sourcePath));
        record.setExt(CommonTextUtils.trimToNull(ext));
        record.setTemplateStatus(PromptTemplateConstants.TEMPLATE_STATUS_ENABLED);
        record.setTenantId(tenantId);
        record.setOwnerUserId(ownerUserId);
        record.setOwnerUserName(ownerUserName);
        return record;
    }

    /**
     * 将更新请求中的可变字段合并到已有模板记录。
     */
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
        record.setTemplateName(CommonTextUtils.trim(templateName));
        record.setDescription(CommonTextUtils.trimToNull(description));
        record.setSourceType(sourceType);
        record.setTemplateContent(templateContent);
        record.setSourcePath(CommonTextUtils.trimToNull(sourcePath));
        record.setExt(CommonTextUtils.trimToNull(ext));
        if (CommonTextUtils.trimToNull(templateStatus) != null) {
            record.setTemplateStatus(templateStatus);
        }
    }

    /**
     * 将模板记录和扩展配置组装为接口响应对象。
     */
    public static PromptTemplateResponse toResponse(
            PromptTemplateRecord record,
            List<PromptTemplateVariableDTO> variableDefinitions,
            PromptTemplateEnterpriseConfigDTO enterpriseConfig
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
                .enterpriseConfig(enterpriseConfig)
                .createTime(CommonTextUtils.toEpochMilli(record.getCreateTime()))
                .updateTime(CommonTextUtils.toEpochMilli(record.getUpdateTime()))
                .build();
    }

    /**
     * 根据模板记录集合生成统计信息。
     */
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

    /**
     * 将本地时间转换为毫秒时间戳。
     */
    private static String normalizeCode(String templateCode) {
        String normalizedCode = CommonTextUtils.trim(templateCode);
        return normalizedCode == null ? null : normalizedCode.toUpperCase();
    }
}
