package com.spring.ai.prompt.application.manager;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.PromptTemplateRecord;
import com.spring.ai.prompt.config.PromptTemplateConstants;
import com.spring.ai.prompt.domain.dto.PromptTemplateBindDTO;
import com.spring.ai.prompt.domain.dto.PromptTemplateResolvedDTO;
import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptTemplateResolver {

    private static final Pattern TEMPLATE_VARIABLE_PATTERN =
            Pattern.compile(PromptTemplateConstants.TEMPLATE_VARIABLE_PATTERN);

    @Resource
    private PromptTemplateSupportManager promptTemplateSupportManager;

    public PromptTemplateResolvedDTO resolveTemplateById(Long promptTemplateId, Map<String, String> promptVariables) {
        PromptTemplateRecord record = promptTemplateSupportManager.requirePromptTemplate(promptTemplateId);
        if (!PromptTemplateConstants.TEMPLATE_STATUS_ENABLED.equals(record.getTemplateStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "prompt template is disabled");
        }
        String promptContent = resolveContent(record.getSourceType(), record.getTemplateContent(), record.getSourcePath());
        java.util.List<PromptTemplateVariableDTO> variableDefinitions =
                promptTemplateSupportManager.parseVariableDefinitions(record.getExt());
        Map<String, String> normalizedVariables = resolvePromptVariables(variableDefinitions, promptVariables);
        return PromptTemplateResolvedDTO.builder()
                .promptTemplateId(record.getId())
                .promptTemplateCode(record.getTemplateCode())
                .promptTemplateName(record.getTemplateName())
                .promptBindingType(PromptTemplateConstants.BINDING_TYPE_TEMPLATE)
                .promptSourceType(record.getSourceType())
                .promptTemplatePath(record.getSourcePath())
                .variableDefinitions(variableDefinitions)
                .promptVariables(normalizedVariables)
                .effectiveSystemPrompt(renderTemplateContent(promptContent, normalizedVariables))
                .build();
    }

    public PromptTemplateResolvedDTO resolveCustomTemplate(PromptTemplateBindDTO bindDTO) {
        String sourceType = normalizeSourceType(bindDTO.getPromptSourceType());
        String promptContent = resolveContent(sourceType, bindDTO.getPromptTemplateContent(), bindDTO.getPromptTemplatePath());
        return PromptTemplateResolvedDTO.builder()
                .promptBindingType(PromptTemplateConstants.BINDING_TYPE_CUSTOM)
                .promptSourceType(sourceType)
                .promptTemplatePath(sourceType.equals(PromptTemplateConstants.SOURCE_TYPE_FILE)
                        ? bindDTO.getPromptTemplatePath().trim()
                        : null)
                .variableDefinitions(java.util.List.of())
                .promptVariables(java.util.Map.of())
                .effectiveSystemPrompt(promptContent)
                .build();
    }

    public String resolveContent(String sourceType, String templateContent, String sourcePath) {
        String normalizedSourceType = normalizeSourceType(sourceType);
        if (PromptTemplateConstants.SOURCE_TYPE_INLINE.equals(normalizedSourceType)) {
            return validateInlineContent(templateContent);
        }
        return readPromptFile(sourcePath);
    }

    public String normalizeSourceType(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "sourceType must not be blank");
        }
        String normalizedSourceType = sourceType.trim().toUpperCase(Locale.ROOT);
        if (!PromptTemplateConstants.SOURCE_TYPE_INLINE.equals(normalizedSourceType)
                && !PromptTemplateConstants.SOURCE_TYPE_FILE.equals(normalizedSourceType)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "unsupported prompt sourceType: " + sourceType);
        }
        return normalizedSourceType;
    }

    public String validateInlineContent(String templateContent) {
        if (!StringUtils.hasText(templateContent)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "templateContent must not be blank");
        }
        String normalizedContent = templateContent.trim();
        if (normalizedContent.length() > PromptTemplateConstants.MAX_TEMPLATE_LENGTH) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "templateContent exceeds max length");
        }
        validateTemplateVariables(normalizedContent);
        return normalizedContent;
    }

    public String readPromptFile(String sourcePath) {
        if (!StringUtils.hasText(sourcePath)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "sourcePath must not be blank");
        }
        Path promptFilePath = Paths.get(sourcePath.trim()).normalize();
        if (!promptFilePath.isAbsolute()) {
            promptFilePath = Paths.get("").toAbsolutePath().resolve(promptFilePath).normalize();
        }
        if (!Files.exists(promptFilePath) || !Files.isRegularFile(promptFilePath)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "prompt template file not found: " + promptFilePath);
        }
        if (!Files.isReadable(promptFilePath)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "prompt template file is not readable: " + promptFilePath);
        }
        String fileName = promptFilePath.getFileName().toString().toLowerCase(Locale.ROOT);
        boolean validExtension = PromptTemplateConstants.ALLOWED_FILE_EXTENSIONS.stream()
                .anyMatch(fileName::endsWith);
        if (!validExtension) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "prompt template file extension is not allowed");
        }
        try {
            long fileSize = Files.size(promptFilePath);
            if (fileSize > PromptTemplateConstants.MAX_FILE_SIZE) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                        "prompt template file exceeds max size");
            }
            String content = Files.readString(promptFilePath, StandardCharsets.UTF_8).trim();
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                        "prompt template file content is blank");
            }
            if (content.length() > PromptTemplateConstants.MAX_TEMPLATE_LENGTH) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                        "prompt template content exceeds max length");
            }
            validateTemplateVariables(content);
            return content;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to read prompt template file", exception);
        }
    }

    private void validateTemplateVariables(String templateContent) {
        if (!StringUtils.hasText(templateContent)) {
            return;
        }
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(templateContent);
        String normalizedContent = matcher.replaceAll("");
        if (normalizedContent.contains("{{") || normalizedContent.contains("}}")) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "prompt template variable syntax is invalid, expected " + PromptTemplateConstants.TEMPLATE_VARIABLE_RULE);
        }
    }

    private Map<String, String> resolvePromptVariables(
            java.util.List<PromptTemplateVariableDTO> variableDefinitions,
            Map<String, String> promptVariables
    ) {
        if (variableDefinitions == null || variableDefinitions.isEmpty()) {
            return java.util.Map.of();
        }
        Map<String, String> inputVariables = promptVariables == null ? java.util.Map.of() : promptVariables;
        return variableDefinitions.stream()
                .collect(Collectors.toMap(
                        PromptTemplateVariableDTO::getVariableName,
                        item -> resolvePromptVariableValue(item, inputVariables.get(item.getVariableName())),
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
                ));
    }

    private String resolvePromptVariableValue(PromptTemplateVariableDTO variableDefinition, String inputValue) {
        if (StringUtils.hasText(inputValue)) {
            return inputValue.trim();
        }
        if (StringUtils.hasText(variableDefinition.getDefaultValue())) {
            return variableDefinition.getDefaultValue().trim();
        }
        if (Boolean.TRUE.equals(variableDefinition.getRequired())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "prompt variable is required: " + variableDefinition.getVariableName());
        }
        return "";
    }

    private String renderTemplateContent(String templateContent, Map<String, String> promptVariables) {
        if (!StringUtils.hasText(templateContent) || promptVariables == null || promptVariables.isEmpty()) {
            return templateContent;
        }
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(templateContent);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String variableValue = promptVariables.getOrDefault(variableName, "");
            matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(variableValue));
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }
}
