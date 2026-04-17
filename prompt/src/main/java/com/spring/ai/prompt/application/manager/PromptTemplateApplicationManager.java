package com.spring.ai.prompt.application.manager;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.PromptTemplateRecord;
import com.spring.ai.common.repository.service.PromptTemplateRecordService;
import com.spring.ai.prompt.application.assmbler.PromptTemplateAssembler;
import com.spring.ai.prompt.config.PromptTemplateConstants;
import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import com.spring.ai.prompt.domain.request.PromptTemplateCreateRequest;
import com.spring.ai.prompt.domain.request.PromptTemplateUpdateRequest;
import com.spring.ai.prompt.domain.response.PromptTemplateResponse;
import com.spring.ai.prompt.domain.response.PromptTemplateStatisticsResponse;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class PromptTemplateApplicationManager {

    private static final Pattern TEMPLATE_VARIABLE_PATTERN =
            Pattern.compile(PromptTemplateConstants.TEMPLATE_VARIABLE_PATTERN);

    @Resource
    private PromptTemplateRecordService promptTemplateRecordService;

    @Resource
    private PromptTemplateSupportManager promptTemplateSupportManager;

    @Resource
    private PromptTemplateResolver promptTemplateResolver;

    public List<PromptTemplateResponse> listTemplates() {
        return promptTemplateRecordService.listByTenantId(promptTemplateSupportManager.getCurrentTenantId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PromptTemplateResponse getTemplateDetail(Long promptTemplateId) {
        return toResponse(promptTemplateSupportManager.requirePromptTemplate(promptTemplateId));
    }

    public PromptTemplateStatisticsResponse statistics() {
        List<PromptTemplateRecord> records = promptTemplateRecordService.listByTenantId(promptTemplateSupportManager.getCurrentTenantId());
        return PromptTemplateAssembler.toStatistics(records);
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptTemplateResponse createTemplate(PromptTemplateCreateRequest request) {
        validateCreateRequest(request);
        Long tenantId = promptTemplateSupportManager.getCurrentTenantId();
        if (promptTemplateRecordService.getByTemplateCode(tenantId, request.getTemplateCode()) != null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "模板编码已存在");
        }

        String sourceType = promptTemplateResolver.normalizeSourceType(request.getSourceType());
        String templateContent = resolveTemplateContent(sourceType, request.getTemplateContent(), request.getSourcePath());
        String sourcePath = resolveSourcePath(sourceType, request.getSourcePath());
        List<PromptTemplateVariableDTO> variableDefinitions = normalizeVariableDefinitions(
                request.getVariableDefinitions(),
                templateContent
        );
        PromptTemplateRecord record = PromptTemplateAssembler.toCreateRecord(
                tenantId,
                promptTemplateSupportManager.getCurrentUserId(),
                promptTemplateSupportManager.getCurrentUserName(),
                request.getTemplateCode(),
                request.getTemplateName(),
                request.getDescription(),
                sourceType,
                templateContent,
                sourcePath,
                promptTemplateSupportManager.toJson(variableDefinitions)
        );
        promptTemplateRecordService.save(record);
        return toResponse(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptTemplateResponse updateTemplate(Long promptTemplateId, PromptTemplateUpdateRequest request) {
        PromptTemplateRecord record = promptTemplateSupportManager.requirePromptTemplate(promptTemplateId);
        validateUpdateRequest(request);
        String sourceType = promptTemplateResolver.normalizeSourceType(request.getSourceType());
        String templateContent = resolveTemplateContent(sourceType, request.getTemplateContent(), request.getSourcePath());
        String sourcePath = resolveSourcePath(sourceType, request.getSourcePath());
        List<PromptTemplateVariableDTO> variableDefinitions = normalizeVariableDefinitions(
                request.getVariableDefinitions(),
                templateContent
        );
        PromptTemplateAssembler.mergeForUpdate(
                record,
                request.getTemplateName(),
                request.getDescription(),
                sourceType,
                templateContent,
                sourcePath,
                StringUtils.hasText(request.getTemplateStatus()) ? normalizeTemplateStatus(request.getTemplateStatus()) : null,
                promptTemplateSupportManager.toJson(variableDefinitions)
        );
        promptTemplateRecordService.updateById(record);
        return toResponse(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long promptTemplateId) {
        promptTemplateRecordService.removeById(promptTemplateSupportManager.requirePromptTemplate(promptTemplateId).getId());
    }

    private void validateCreateRequest(PromptTemplateCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getTemplateCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "模板编码不能为空");
        }
        if (!StringUtils.hasText(request.getTemplateName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "模板名称不能为空");
        }
    }

    private void validateUpdateRequest(PromptTemplateUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getTemplateName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "模板名称不能为空");
        }
    }

    private String resolveTemplateContent(String sourceType, String templateContent, String sourcePath) {
        if (PromptTemplateConstants.SOURCE_TYPE_INLINE.equals(sourceType)) {
            return promptTemplateResolver.validateInlineContent(templateContent);
        }
        return promptTemplateResolver.readPromptFile(sourcePath);
    }

    private String resolveSourcePath(String sourceType, String sourcePath) {
        if (!PromptTemplateConstants.SOURCE_TYPE_FILE.equals(sourceType) || !StringUtils.hasText(sourcePath)) {
            return null;
        }
        return sourcePath.trim();
    }

    private String normalizeTemplateStatus(String templateStatus) {
        String normalizedTemplateStatus = templateStatus.trim().toUpperCase(Locale.ROOT);
        if (!PromptTemplateConstants.TEMPLATE_STATUS_ENABLED.equals(normalizedTemplateStatus)
                && !PromptTemplateConstants.TEMPLATE_STATUS_DISABLED.equals(normalizedTemplateStatus)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "不支持的模板状态：" + templateStatus);
        }
        return normalizedTemplateStatus;
    }

    private PromptTemplateResponse toResponse(PromptTemplateRecord record) {
        return PromptTemplateAssembler.toResponse(
                record,
                promptTemplateSupportManager.parseVariableDefinitions(record.getExt())
        );
    }

    private List<PromptTemplateVariableDTO> normalizeVariableDefinitions(
            List<PromptTemplateVariableDTO> variableDefinitions,
            String templateContent
    ) {
        Map<String, Boolean> placeholderMap = extractTemplateVariables(templateContent);
        if (placeholderMap.isEmpty()) {
            return List.of();
        }
        if (variableDefinitions == null || variableDefinitions.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "模板变量已声明占位符时，必须同步提供变量定义");
        }
        Map<String, PromptTemplateVariableDTO> variableMap = variableDefinitions.stream()
                .peek(this::validateVariableDefinition)
                .collect(java.util.stream.Collectors.toMap(
                        item -> item.getVariableName().trim(),
                        item -> item,
                        (left, right) -> {
                            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                                    "变量定义重复：" + left.getVariableName());
                        }
                ));
        Set<String> placeholderNames = placeholderMap.keySet();
        if (!variableMap.keySet().equals(placeholderNames)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "变量定义必须与模板占位符完全一致");
        }
        return variableMap.values().stream()
                .map(item -> PromptTemplateVariableDTO.builder()
                        .variableName(item.getVariableName().trim())
                        .required(Boolean.TRUE.equals(item.getRequired()))
                        .defaultValue(trimToNull(item.getDefaultValue()))
                        .description(trimToNull(item.getDescription()))
                        .build())
                .toList();
    }

    private Map<String, Boolean> extractTemplateVariables(String templateContent) {
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(templateContent == null ? "" : templateContent);
        Map<String, Boolean> variableMap = new java.util.LinkedHashMap<>();
        while (matcher.find()) {
            variableMap.put(matcher.group(1), Boolean.TRUE);
        }
        return variableMap;
    }

    private void validateVariableDefinition(PromptTemplateVariableDTO variableDefinition) {
        if (variableDefinition == null || !StringUtils.hasText(variableDefinition.getVariableName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "变量名不能为空");
        }
        if (!variableDefinition.getVariableName().trim().matches("[a-zA-Z][a-zA-Z0-9_]*")) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "变量名格式不合法：" + variableDefinition.getVariableName());
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

}
