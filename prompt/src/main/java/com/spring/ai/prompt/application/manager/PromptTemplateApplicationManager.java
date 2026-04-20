package com.spring.ai.prompt.application.manager;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.PromptTemplateRecord;
import com.spring.ai.common.repository.service.PromptTemplateRecordService;
import com.spring.ai.prompt.application.assmbler.PromptTemplateAssembler;
import com.spring.ai.prompt.config.PromptTemplateConstants;
import com.spring.ai.prompt.domain.dto.PromptTemplateEnterpriseConfigDTO;
import com.spring.ai.prompt.domain.dto.PromptTemplateExtDTO;
import com.spring.ai.prompt.domain.dto.PromptTemplateVariableDTO;
import com.spring.ai.prompt.domain.request.PromptTemplateCreateRequest;
import com.spring.ai.prompt.domain.request.PromptTemplateRenderRequest;
import com.spring.ai.prompt.domain.request.PromptTemplateUpdateRequest;
import com.spring.ai.prompt.domain.response.PromptTemplateRenderResponse;
import com.spring.ai.prompt.domain.response.PromptTemplateResponse;
import com.spring.ai.prompt.domain.response.PromptTemplateStatisticsResponse;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

/**
 * 文件用途：提示词模板应用层管理器。
 * 核心功能：负责模板 CRUD、企业配置保存、变量校验与试渲染能力编排。
 */
@Component
public class PromptTemplateApplicationManager {

    private static final Pattern TEMPLATE_VARIABLE_PATTERN =
            Pattern.compile(PromptTemplateConstants.TEMPLATE_VARIABLE_PATTERN);
    private static final Pattern CONDITIONAL_VARIABLE_PATTERN =
            Pattern.compile("\\{\\{#if\\s+([^}]+?)\\s*}}");
    private static final Pattern LOOP_VARIABLE_PATTERN =
            Pattern.compile("\\{\\{#each\\s+([a-zA-Z][a-zA-Z0-9_]*)\\s*}}");

    @Resource
    private PromptTemplateRecordService promptTemplateRecordService;

    @Resource
    private PromptTemplateSupportManager promptTemplateSupportManager;

    @Resource
    private PromptTemplateResolver promptTemplateResolver;

    /**
     * 查询当前租户下的模板列表。
     */
    public List<PromptTemplateResponse> listTemplates() {
        return promptTemplateRecordService.listByTenantId(promptTemplateSupportManager.getCurrentTenantId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 查询单个模板详情。
     */
    public PromptTemplateResponse getTemplateDetail(Long promptTemplateId) {
        return toResponse(promptTemplateSupportManager.requirePromptTemplate(promptTemplateId));
    }

    /**
     * 统计当前租户下模板数量及来源分布。
     */
    public PromptTemplateStatisticsResponse statistics() {
        List<PromptTemplateRecord> records = promptTemplateRecordService.listByTenantId(
                promptTemplateSupportManager.getCurrentTenantId());
        return PromptTemplateAssembler.toStatistics(records);
    }

    /**
     * 使用请求变量对模板执行试渲染。
     */
    public PromptTemplateRenderResponse renderTemplate(Long promptTemplateId, PromptTemplateRenderRequest request) {
        PromptTemplateRecord record = promptTemplateSupportManager.requirePromptTemplate(promptTemplateId);
        PromptTemplateExtDTO ext = promptTemplateSupportManager.parseTemplateExt(record.getExt());
        String templateContent = promptTemplateResolver.resolveContent(
                record.getSourceType(),
                record.getTemplateContent(),
                record.getSourcePath()
        );
        PromptTemplateResolver.TemplateRenderResult renderResult = promptTemplateResolver.renderEnterpriseTemplate(
                templateContent,
                request == null ? Map.of() : request.getVariables(),
                ext.getVariableDefinitions()
        );
        return PromptTemplateRenderResponse.builder()
                .renderedContent(renderResult.getRenderedContent())
                .missingVariables(renderResult.getMissingVariables())
                .appliedConditions(renderResult.getAppliedConditions())
                .appliedLoops(renderResult.getAppliedLoops())
                .build();
    }

    /**
     * 创建提示词模板，并同步保存变量定义和企业扩展配置。
     */
    @Transactional(rollbackFor = Exception.class)
    public PromptTemplateResponse createTemplate(PromptTemplateCreateRequest request) {
        validateCreateRequest(request);
        Long tenantId = promptTemplateSupportManager.getCurrentTenantId();
        if (promptTemplateRecordService.getByTemplateCode(tenantId, request.getTemplateCode()) != null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "模板编码已存在");
        }

        String sourceType = promptTemplateResolver.normalizeSourceType(request.getSourceType());
        String templateContent = resolveTemplateContent(sourceType, request.getTemplateContent(), request.getSourcePath());
        String sourcePath = resolveSourcePath(sourceType, request.getSourcePath());
        List<PromptTemplateVariableDTO> variableDefinitions = normalizeVariableDefinitions(
                request.getVariableDefinitions(),
                templateContent
        );
        PromptTemplateEnterpriseConfigDTO enterpriseConfig = normalizeEnterpriseConfig(request.getEnterpriseConfig());
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
                promptTemplateSupportManager.buildTemplateExtJson(variableDefinitions, enterpriseConfig)
        );
        promptTemplateRecordService.save(record);
        return toResponse(record);
    }

    /**
     * 更新已有提示词模板的基础信息、来源配置和扩展规则。
     */
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
        PromptTemplateEnterpriseConfigDTO enterpriseConfig = normalizeEnterpriseConfig(request.getEnterpriseConfig());
        PromptTemplateAssembler.mergeForUpdate(
                record,
                request.getTemplateName(),
                request.getDescription(),
                sourceType,
                templateContent,
                sourcePath,
                StringUtils.hasText(request.getTemplateStatus()) ? normalizeTemplateStatus(request.getTemplateStatus()) : null,
                promptTemplateSupportManager.buildTemplateExtJson(variableDefinitions, enterpriseConfig)
        );
        promptTemplateRecordService.updateById(record);
        return toResponse(record);
    }

    /**
     * 删除当前租户下的模板记录。
     */
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
        PromptTemplateExtDTO ext = promptTemplateSupportManager.parseTemplateExt(record.getExt());
        return PromptTemplateAssembler.toResponse(record, ext.getVariableDefinitions(), ext.getEnterpriseConfig());
    }

    private List<PromptTemplateVariableDTO> normalizeVariableDefinitions(
            List<PromptTemplateVariableDTO> variableDefinitions,
            String templateContent
    ) {
        // 模板正文中的占位符必须和变量定义保持一致，避免出现渲染时缺参或冗余定义。
        Map<String, Boolean> placeholderMap = extractTemplateVariables(templateContent);
        if (placeholderMap.isEmpty()) {
            return variableDefinitions == null ? List.of() : variableDefinitions.stream()
                    .peek(this::validateVariableDefinition)
                    .map(this::normalizeVariableDefinition)
                    .toList();
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
        List<String> missingVariables = placeholderNames.stream()
                .filter(item -> !variableMap.containsKey(item))
                .toList();
        if (!missingVariables.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "变量定义缺少必要占位符：" + String.join("、", missingVariables));
        }
        return variableDefinitions.stream()
                .map(this::normalizeVariableDefinition)
                .toList();
    }

    private Map<String, Boolean> extractTemplateVariables(String templateContent) {
        Map<String, Boolean> variableMap = new LinkedHashMap<>();
        String content = templateContent == null ? "" : templateContent;
        Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(content);
        // 普通占位符、条件分支和循环块会引用不同形式的变量，这里统一提取根变量名。
        while (matcher.find()) {
            String variableName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            addRootVariable(variableMap, variableName);
        }
        collectConditionalVariables(content, variableMap);
        collectLoopVariables(content, variableMap);
        return variableMap;
    }

    private void collectConditionalVariables(String templateContent, Map<String, Boolean> variableMap) {
        Matcher matcher = CONDITIONAL_VARIABLE_PATTERN.matcher(templateContent);
        while (matcher.find()) {
            String expression = matcher.group(1);
            String leftExpression = expression.split("==|!=", 2)[0].trim();
            addRootVariable(variableMap, leftExpression);
        }
    }

    private void collectLoopVariables(String templateContent, Map<String, Boolean> variableMap) {
        Matcher matcher = LOOP_VARIABLE_PATTERN.matcher(templateContent);
        while (matcher.find()) {
            addRootVariable(variableMap, matcher.group(1));
        }
    }

    private void addRootVariable(Map<String, Boolean> variableMap, String variableName) {
        if (!StringUtils.hasText(variableName)) {
            return;
        }
        String rootVariable = variableName.trim().split("\\.", 2)[0];
        if ("item".equals(rootVariable) || "index".equals(rootVariable)) {
            return;
        }
        if (rootVariable.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
            variableMap.put(rootVariable, Boolean.TRUE);
        }
    }

    private void validateVariableDefinition(PromptTemplateVariableDTO variableDefinition) {
        if (variableDefinition == null || !StringUtils.hasText(variableDefinition.getVariableName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, "变量名不能为空");
        }
        if (!variableDefinition.getVariableName().trim().matches("[a-zA-Z][a-zA-Z0-9_]*")) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    "变量名格式不合法：" + variableDefinition.getVariableName());
        }
    }

    private PromptTemplateVariableDTO normalizeVariableDefinition(PromptTemplateVariableDTO item) {
        return PromptTemplateVariableDTO.builder()
                .variableName(item.getVariableName().trim())
                .required(Boolean.TRUE.equals(item.getRequired()))
                .defaultValue(trimToNull(item.getDefaultValue()))
                .description(trimToNull(item.getDescription()))
                .build();
    }

    private PromptTemplateEnterpriseConfigDTO normalizeEnterpriseConfig(PromptTemplateEnterpriseConfigDTO enterpriseConfig) {
        if (enterpriseConfig == null) {
            return null;
        }
        // 扩展配置会被序列化存入 ext 字段，这里先做空值裁剪和列表归一化。
        PromptTemplateEnterpriseConfigDTO normalizedConfig = new PromptTemplateEnterpriseConfigDTO();
        normalizedConfig.setRendering(normalizeRenderingPolicy(enterpriseConfig.getRendering()));
        normalizedConfig.setRolePolicy(normalizeRolePolicy(enterpriseConfig.getRolePolicy()));
        normalizedConfig.setWorkflowPolicy(normalizeWorkflowPolicy(enterpriseConfig.getWorkflowPolicy()));
        normalizedConfig.setSecurityPolicy(normalizeSecurityPolicy(enterpriseConfig.getSecurityPolicy()));
        normalizedConfig.setAssetPolicy(normalizeAssetPolicy(enterpriseConfig.getAssetPolicy()));
        normalizedConfig.setOutputPolicy(normalizeOutputPolicy(enterpriseConfig.getOutputPolicy()));
        normalizedConfig.setContextPolicy(normalizeContextPolicy(enterpriseConfig.getContextPolicy()));
        normalizedConfig.setFallbackPolicy(normalizeFallbackPolicy(enterpriseConfig.getFallbackPolicy()));
        normalizedConfig.setObservabilityPolicy(normalizeObservabilityPolicy(enterpriseConfig.getObservabilityPolicy()));
        normalizedConfig.setIntegrationPolicy(normalizeIntegrationPolicy(enterpriseConfig.getIntegrationPolicy()));
        return normalizedConfig;
    }

    private PromptTemplateEnterpriseConfigDTO.RenderingPolicy normalizeRenderingPolicy(
            PromptTemplateEnterpriseConfigDTO.RenderingPolicy policy
    ) {
        if (policy == null) {
            return null;
        }
        return PromptTemplateEnterpriseConfigDTO.RenderingPolicy.builder()
                .dynamicVariables(normalizeStringList(policy.getDynamicVariables()))
                .dataSources(normalizeStringList(policy.getDataSources()))
                .conditionalBranches(policy.getConditionalBranches() == null ? List.of() : policy.getConditionalBranches().stream()
                        .map(item -> PromptTemplateEnterpriseConfigDTO.ConditionalRule.builder()
                                .name(trimToNull(item.getName()))
                                .conditionExpression(trimToNull(item.getConditionExpression()))
                                .trueTemplate(trimToNull(item.getTrueTemplate()))
                                .falseTemplate(trimToNull(item.getFalseTemplate()))
                                .build())
                        .toList())
                .loopRenderers(policy.getLoopRenderers() == null ? List.of() : policy.getLoopRenderers().stream()
                        .map(item -> PromptTemplateEnterpriseConfigDTO.LoopRule.builder()
                                .listVariable(trimToNull(item.getListVariable()))
                                .itemAlias(trimToNull(item.getItemAlias()))
                                .emptyTemplate(trimToNull(item.getEmptyTemplate()))
                                .itemTemplate(trimToNull(item.getItemTemplate()))
                                .build())
                        .toList())
                .build();
    }

    private PromptTemplateEnterpriseConfigDTO.RolePolicy normalizeRolePolicy(PromptTemplateEnterpriseConfigDTO.RolePolicy policy) {
        if (policy == null) {
            return null;
        }
        return PromptTemplateEnterpriseConfigDTO.RolePolicy.builder()
                .agentRole(trimToNull(policy.getAgentRole()))
                .dutyScope(trimToNull(policy.getDutyScope()))
                .forbiddenActions(normalizeStringList(policy.getForbiddenActions()))
                .tone(trimToNull(policy.getTone()))
                .speechRules(normalizeStringList(policy.getSpeechRules()))
                .build();
    }

    private PromptTemplateEnterpriseConfigDTO.WorkflowPolicy normalizeWorkflowPolicy(
            PromptTemplateEnterpriseConfigDTO.WorkflowPolicy policy
    ) {
        if (policy == null) {
            return null;
        }
        return PromptTemplateEnterpriseConfigDTO.WorkflowPolicy.builder()
                .workflowStages(normalizeStringList(policy.getWorkflowStages()))
                .hardRules(normalizeStringList(policy.getHardRules()))
                .toolRules(policy.getToolRules() == null ? List.of() : policy.getToolRules().stream()
                        .map(item -> PromptTemplateEnterpriseConfigDTO.ToolRule.builder()
                                .toolCode(trimToNull(item.getToolCode()))
                                .triggerCondition(trimToNull(item.getTriggerCondition()))
                                .parameterSpec(trimToNull(item.getParameterSpec()))
                                .permissionScope(trimToNull(item.getPermissionScope()))
                                .build())
                        .toList())
                .build();
    }

    private PromptTemplateEnterpriseConfigDTO.SecurityPolicy normalizeSecurityPolicy(
            PromptTemplateEnterpriseConfigDTO.SecurityPolicy policy
    ) {
        if (policy == null) {
            return null;
        }
        return PromptTemplateEnterpriseConfigDTO.SecurityPolicy.builder()
                .desensitizationRules(normalizeStringList(policy.getDesensitizationRules()))
                .antiInjectionRules(normalizeStringList(policy.getAntiInjectionRules()))
                .complianceBlacklist(normalizeStringList(policy.getComplianceBlacklist()))
                .permissionTiers(normalizeStringList(policy.getPermissionTiers()))
                .build();
    }

    private PromptTemplateEnterpriseConfigDTO.AssetPolicy normalizeAssetPolicy(PromptTemplateEnterpriseConfigDTO.AssetPolicy policy) {
        if (policy == null) {
            return null;
        }
        return PromptTemplateEnterpriseConfigDTO.AssetPolicy.builder()
                .commonModules(normalizeStringList(policy.getCommonModules()))
                .businessModules(normalizeStringList(policy.getBusinessModules()))
                .versionStrategy(trimToNull(policy.getVersionStrategy()))
                .permissionStrategy(trimToNull(policy.getPermissionStrategy()))
                .categories(normalizeStringList(policy.getCategories()))
                .build();
    }

    private PromptTemplateEnterpriseConfigDTO.OutputPolicy normalizeOutputPolicy(
            PromptTemplateEnterpriseConfigDTO.OutputPolicy policy
    ) {
        if (policy == null) {
            return null;
        }
        return PromptTemplateEnterpriseConfigDTO.OutputPolicy.builder()
                .outputFormat(trimToNull(policy.getOutputFormat()))
                .requiredFields(normalizeStringList(policy.getRequiredFields()))
                .maxLength(policy.getMaxLength())
                .channelConstraints(normalizeStringList(policy.getChannelConstraints()))
                .build();
    }

    private PromptTemplateEnterpriseConfigDTO.ContextPolicy normalizeContextPolicy(
            PromptTemplateEnterpriseConfigDTO.ContextPolicy policy
    ) {
        if (policy == null) {
            return null;
        }
        return PromptTemplateEnterpriseConfigDTO.ContextPolicy.builder()
                .historyStrategy(trimToNull(policy.getHistoryStrategy()))
                .memoryFields(normalizeStringList(policy.getMemoryFields()))
                .sessionIsolation(policy.getSessionIsolation())
                .retentionStrategy(trimToNull(policy.getRetentionStrategy()))
                .build();
    }

    private PromptTemplateEnterpriseConfigDTO.FallbackPolicy normalizeFallbackPolicy(
            PromptTemplateEnterpriseConfigDTO.FallbackPolicy policy
    ) {
        if (policy == null) {
            return null;
        }
        return PromptTemplateEnterpriseConfigDTO.FallbackPolicy.builder()
                .fallbackMessages(normalizeStringList(policy.getFallbackMessages()))
                .repeatedRules(normalizeStringList(policy.getRepeatedRules()))
                .supportedLanguages(normalizeStringList(policy.getSupportedLanguages()))
                .resilienceStrategy(trimToNull(policy.getResilienceStrategy()))
                .build();
    }

    private PromptTemplateEnterpriseConfigDTO.ObservabilityPolicy normalizeObservabilityPolicy(
            PromptTemplateEnterpriseConfigDTO.ObservabilityPolicy policy
    ) {
        if (policy == null) {
            return null;
        }
        return PromptTemplateEnterpriseConfigDTO.ObservabilityPolicy.builder()
                .traceEnabled(policy.getTraceEnabled())
                .metricKeys(normalizeStringList(policy.getMetricKeys()))
                .logBindingFields(normalizeStringList(policy.getLogBindingFields()))
                .grayReleaseStrategy(trimToNull(policy.getGrayReleaseStrategy()))
                .build();
    }

    private PromptTemplateEnterpriseConfigDTO.IntegrationPolicy normalizeIntegrationPolicy(
            PromptTemplateEnterpriseConfigDTO.IntegrationPolicy policy
    ) {
        if (policy == null) {
            return null;
        }
        return PromptTemplateEnterpriseConfigDTO.IntegrationPolicy.builder()
                .externalSystems(normalizeStringList(policy.getExternalSystems()))
                .parameterBindings(normalizeStringList(policy.getParameterBindings()))
                .batchScenarios(normalizeStringList(policy.getBatchScenarios()))
                .editorMode(trimToNull(policy.getEditorMode()))
                .build();
    }

    private List<String> normalizeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> normalizedValues = new ArrayList<>();
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                normalizedValues.add(normalized);
            }
        }
        return normalizedValues;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
