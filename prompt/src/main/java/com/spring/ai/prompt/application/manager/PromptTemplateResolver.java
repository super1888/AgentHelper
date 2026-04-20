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
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：解析提示词模板内容与变量。
 * 核心功能：支持模板内容加载、变量补全，以及企业模板的条件分支与循环试渲染。
 */
@Component
public class PromptTemplateResolver {

    private static final Pattern TEMPLATE_VARIABLE_PATTERN =
            Pattern.compile(PromptTemplateConstants.TEMPLATE_VARIABLE_PATTERN);
    private static final Pattern CONDITIONAL_BLOCK_PATTERN =
            Pattern.compile("\\{\\{#if\\s+(.+?)\\s*}}(.*?)(?:\\{\\{else}}(.*?))?\\{\\{/if}}", Pattern.DOTALL);
    private static final Pattern LOOP_BLOCK_PATTERN =
            Pattern.compile("\\{\\{#each\\s+([a-zA-Z][a-zA-Z0-9_]*)\\s*}}(.*?)\\{\\{/each}}", Pattern.DOTALL);

    @Resource
    private PromptTemplateSupportManager promptTemplateSupportManager;

    /**
     * 根据模板主键解析出最终生效的系统提示词内容。
     */
    public PromptTemplateResolvedDTO resolveTemplateById(Long promptTemplateId, Map<String, String> promptVariables) {
        PromptTemplateRecord record = promptTemplateSupportManager.requirePromptTemplate(promptTemplateId);
        if (!PromptTemplateConstants.TEMPLATE_STATUS_ENABLED.equals(record.getTemplateStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "prompt template is disabled");
        }
        String promptContent = resolveContent(record.getSourceType(), record.getTemplateContent(), record.getSourcePath());
        List<PromptTemplateVariableDTO> variableDefinitions = promptTemplateSupportManager.parseVariableDefinitions(record.getExt());
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
                .effectiveSystemPrompt(renderEnterpriseTemplate(promptContent, new LinkedHashMap<>(normalizedVariables), variableDefinitions)
                        .getRenderedContent())
                .build();
    }

    /**
     * 解析自定义绑定模板，适用于未落库的临时模板配置。
     */
    public PromptTemplateResolvedDTO resolveCustomTemplate(PromptTemplateBindDTO bindDTO) {
        String sourceType = normalizeSourceType(bindDTO.getPromptSourceType());
        String promptContent = resolveContent(sourceType, bindDTO.getPromptTemplateContent(), bindDTO.getPromptTemplatePath());
        return PromptTemplateResolvedDTO.builder()
                .promptBindingType(PromptTemplateConstants.BINDING_TYPE_CUSTOM)
                .promptSourceType(sourceType)
                .promptTemplatePath(sourceType.equals(PromptTemplateConstants.SOURCE_TYPE_FILE)
                        ? bindDTO.getPromptTemplatePath().trim()
                        : null)
                .variableDefinitions(List.of())
                .promptVariables(Map.of())
                .effectiveSystemPrompt(promptContent)
                .build();
    }

    /**
     * 按来源类型获取模板正文。
     */
    public String resolveContent(String sourceType, String templateContent, String sourcePath) {
        String normalizedSourceType = normalizeSourceType(sourceType);
        if (PromptTemplateConstants.SOURCE_TYPE_INLINE.equals(normalizedSourceType)) {
            return validateInlineContent(templateContent);
        }
        return readPromptFile(sourcePath);
    }

    /**
     * 规范化模板来源类型并校验合法性。
     */
    public String normalizeSourceType(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "来源类型不能为空");
        }
        String normalizedSourceType = sourceType.trim().toUpperCase(Locale.ROOT);
        if (!PromptTemplateConstants.SOURCE_TYPE_INLINE.equals(normalizedSourceType)
                && !PromptTemplateConstants.SOURCE_TYPE_FILE.equals(normalizedSourceType)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "unsupported prompt sourceType: " + sourceType);
        }
        return normalizedSourceType;
    }

    /**
     * 校验内联模板正文并做长度限制。
     */
    public String validateInlineContent(String templateContent) {
        if (!StringUtils.hasText(templateContent)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "templateContent must not be blank");
        }
        String normalizedContent = templateContent.trim();
        if (normalizedContent.length() > PromptTemplateConstants.MAX_TEMPLATE_LENGTH) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "templateContent exceeds max length");
        }
        return normalizedContent;
    }

    /**
     * 从文件系统读取模板内容，并校验路径、后缀和体积限制。
     */
    public String readPromptFile(String sourcePath) {
        if (!StringUtils.hasText(sourcePath)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "sourcePath must not be blank");
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
            return content;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to read prompt template file", exception);
        }
    }

    public TemplateRenderResult renderEnterpriseTemplate(
            String templateContent,
            Map<String, Object> renderVariables,
            List<PromptTemplateVariableDTO> variableDefinitions
    ) {
        Map<String, Object> normalizedVariables = renderVariables == null ? Map.of() : renderVariables;
        LinkedHashSet<String> missingVariables = new LinkedHashSet<>();
        LinkedHashSet<String> appliedConditions = new LinkedHashSet<>();
        LinkedHashSet<String> appliedLoops = new LinkedHashSet<>();
        Map<String, Object> resolvedVariables = applyDefaults(variableDefinitions, normalizedVariables, missingVariables);
        String renderedContent = renderTemplateContent(
                templateContent,
                resolvedVariables,
                missingVariables,
                appliedConditions,
                appliedLoops
        );
        return TemplateRenderResult.builder()
                .renderedContent(renderedContent)
                .missingVariables(new ArrayList<>(missingVariables))
                .appliedConditions(new ArrayList<>(appliedConditions))
                .appliedLoops(new ArrayList<>(appliedLoops))
                .build();
    }

    private Map<String, Object> applyDefaults(
            List<PromptTemplateVariableDTO> variableDefinitions,
            Map<String, Object> renderVariables,
            LinkedHashSet<String> missingVariables
    ) {
        Map<String, Object> resolvedVariables = new LinkedHashMap<>();
        if (renderVariables != null) {
            resolvedVariables.putAll(renderVariables);
        }
        if (variableDefinitions == null || variableDefinitions.isEmpty()) {
            return resolvedVariables;
        }
        for (PromptTemplateVariableDTO variableDefinition : variableDefinitions) {
            String variableName = variableDefinition.getVariableName();
            Object inputValue = resolvedVariables.get(variableName);
            if (hasValue(inputValue)) {
                continue;
            }
            if (StringUtils.hasText(variableDefinition.getDefaultValue())) {
                resolvedVariables.put(variableName, variableDefinition.getDefaultValue().trim());
                continue;
            }
            if (Boolean.TRUE.equals(variableDefinition.getRequired())) {
                missingVariables.add(variableName);
            }
        }
        return resolvedVariables;
    }

    private Map<String, String> resolvePromptVariables(
            List<PromptTemplateVariableDTO> variableDefinitions,
            Map<String, String> promptVariables
    ) {
        if (variableDefinitions == null || variableDefinitions.isEmpty()) {
            return Map.of();
        }
        Map<String, String> inputVariables = promptVariables == null ? Map.of() : promptVariables;
        return variableDefinitions.stream()
                .collect(Collectors.toMap(
                        PromptTemplateVariableDTO::getVariableName,
                        item -> resolvePromptVariableValue(item, inputVariables.get(item.getVariableName())),
                        (left, right) -> right,
                        LinkedHashMap::new
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

    private String renderTemplateContent(
            String templateContent,
            Map<String, Object> promptVariables,
            LinkedHashSet<String> missingVariables,
            LinkedHashSet<String> appliedConditions,
            LinkedHashSet<String> appliedLoops
    ) {
        if (!StringUtils.hasText(templateContent)) {
            return templateContent;
        }
        String contentAfterLoops = renderLoopBlocks(templateContent, promptVariables, missingVariables, appliedConditions, appliedLoops);
        String contentAfterConditions = renderConditionalBlocks(
                contentAfterLoops,
                promptVariables,
                missingVariables,
                appliedConditions,
                appliedLoops
        );
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(contentAfterConditions);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            Object variableValue = resolvePathValue(promptVariables, variableName);
            if (!hasValue(variableValue)) {
                missingVariables.add(variableName);
            }
            matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(stringifyValue(variableValue)));
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    private String renderLoopBlocks(
            String templateContent,
            Map<String, Object> promptVariables,
            LinkedHashSet<String> missingVariables,
            LinkedHashSet<String> appliedConditions,
            LinkedHashSet<String> appliedLoops
    ) {
        String renderedContent = templateContent;
        Matcher matcher = LOOP_BLOCK_PATTERN.matcher(renderedContent);
        while (matcher.find()) {
            String listVariable = matcher.group(1);
            String itemTemplate = matcher.group(2);
            Object listValue = resolvePathValue(promptVariables, listVariable);
            List<?> items = convertToList(listValue);
            appliedLoops.add(listVariable + ":" + items.size());
            StringBuilder blockBuilder = new StringBuilder();
            for (int index = 0; index < items.size(); index++) {
                Object item = items.get(index);
                Map<String, Object> childVariables = new LinkedHashMap<>(promptVariables == null ? Map.of() : promptVariables);
                childVariables.put("item", item);
                childVariables.put("index", index);
                blockBuilder.append(renderTemplateContent(
                        itemTemplate,
                        childVariables,
                        missingVariables,
                        appliedConditions,
                        appliedLoops
                ));
            }
            renderedContent = matcher.replaceFirst(Matcher.quoteReplacement(blockBuilder.toString()));
            matcher = LOOP_BLOCK_PATTERN.matcher(renderedContent);
        }
        return renderedContent;
    }

    private String renderConditionalBlocks(
            String templateContent,
            Map<String, Object> promptVariables,
            LinkedHashSet<String> missingVariables,
            LinkedHashSet<String> appliedConditions,
            LinkedHashSet<String> appliedLoops
    ) {
        String renderedContent = templateContent;
        Matcher matcher = CONDITIONAL_BLOCK_PATTERN.matcher(renderedContent);
        while (matcher.find()) {
            String expression = matcher.group(1);
            String trueContent = matcher.group(2);
            String falseContent = matcher.group(3);
            boolean matched = evaluateCondition(expression, promptVariables, missingVariables);
            appliedConditions.add(expression + "=" + matched);
            String selectedContent = matched ? trueContent : (falseContent == null ? "" : falseContent);
            String replacement = renderTemplateContent(
                    selectedContent,
                    promptVariables,
                    missingVariables,
                    appliedConditions,
                    appliedLoops
            );
            renderedContent = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
            matcher = CONDITIONAL_BLOCK_PATTERN.matcher(renderedContent);
        }
        return renderedContent;
    }

    private boolean evaluateCondition(
            String expression,
            Map<String, Object> promptVariables,
            LinkedHashSet<String> missingVariables
    ) {
        String normalizedExpression = expression == null ? "" : expression.trim();
        if (!StringUtils.hasText(normalizedExpression)) {
            return false;
        }
        if (normalizedExpression.contains("==")) {
            String[] parts = normalizedExpression.split("==", 2);
            Object actualValue = resolvePathValue(promptVariables, parts[0].trim());
            if (!hasValue(actualValue)) {
                missingVariables.add(parts[0].trim());
            }
            return stringifyValue(actualValue).equals(stripQuotes(parts[1].trim()));
        }
        if (normalizedExpression.contains("!=")) {
            String[] parts = normalizedExpression.split("!=", 2);
            Object actualValue = resolvePathValue(promptVariables, parts[0].trim());
            if (!hasValue(actualValue)) {
                missingVariables.add(parts[0].trim());
            }
            return !stringifyValue(actualValue).equals(stripQuotes(parts[1].trim()));
        }
        Object actualValue = resolvePathValue(promptVariables, normalizedExpression);
        if (!hasValue(actualValue)) {
            missingVariables.add(normalizedExpression);
        }
        if (actualValue instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return hasValue(actualValue)
                && !"false".equalsIgnoreCase(String.valueOf(actualValue))
                && !"0".equals(String.valueOf(actualValue));
    }

    private Object resolvePathValue(Map<String, Object> promptVariables, String path) {
        if (promptVariables == null || !StringUtils.hasText(path)) {
            return null;
        }
        String[] pathSegments = path.trim().split("\\.");
        Object currentValue = promptVariables.get(pathSegments[0]);
        for (int index = 1; index < pathSegments.length; index++) {
            if (currentValue instanceof Map<?, ?> mapValue) {
                currentValue = mapValue.get(pathSegments[index]);
                continue;
            }
            return null;
        }
        return currentValue;
    }

    private List<?> convertToList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> items = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                items.add(Array.get(value, index));
            }
            return items;
        }
        return List.of(value);
    }

    private boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String stringValue) {
            return StringUtils.hasText(stringValue);
        }
        return true;
    }

    private String stringifyValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String stripQuotes(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        String normalizedValue = rawValue.trim();
        if (normalizedValue.length() >= 2
                && ((normalizedValue.startsWith("'") && normalizedValue.endsWith("'"))
                || (normalizedValue.startsWith("\"") && normalizedValue.endsWith("\"")))) {
            return normalizedValue.substring(1, normalizedValue.length() - 1);
        }
        return normalizedValue;
    }

    @lombok.Value
    @lombok.Builder
    public static class TemplateRenderResult {

        String renderedContent;

        List<String> missingVariables;

        List<String> appliedConditions;

        List<String> appliedLoops;
    }
}
