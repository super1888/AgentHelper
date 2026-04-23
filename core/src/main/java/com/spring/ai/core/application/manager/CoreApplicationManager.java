package com.spring.ai.core.application.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.enums.ModelProviderEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.AgentVersion;
import com.spring.ai.common.repository.enitiy.ModelDefinition;
import com.spring.ai.common.repository.enitiy.ModelProviderConfig;
import com.spring.ai.common.repository.service.AgentVersionService;
import com.spring.ai.common.repository.service.ModelDefinitionService;
import com.spring.ai.common.repository.service.ModelProviderConfigService;
import com.spring.ai.common.security.ModelSecretCryptoService;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.core.domain.request.ModelDefinitionSaveRequest;
import com.spring.ai.core.domain.request.ModelProviderConfigSaveRequest;
import com.spring.ai.core.domain.request.ModelProviderTestRequest;
import com.spring.ai.core.domain.request.ModelTestRequest;
import com.spring.ai.core.domain.dto.ChatModelRequest;
import com.spring.ai.core.domain.dto.ChatOptionsDTO;
import com.spring.ai.core.domain.response.ModelDefinitionResponse;
import com.spring.ai.core.domain.response.ModelOptionResponse;
import com.spring.ai.core.domain.response.ModelProviderConfigResponse;
import com.spring.ai.core.domain.response.ModelTestResponse;
import com.spring.ai.core.domain.response.ProviderCatalogResponse;
import com.spring.ai.core.facotry.DynamicChatModelFactory;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class CoreApplicationManager {

    @Resource
    private ModelProviderConfigService modelProviderConfigService;

    @Resource
    private ModelDefinitionService modelDefinitionService;

    @Resource
    private AgentVersionService agentVersionService;

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private ModelSecretCryptoService modelSecretCryptoService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private DynamicChatModelFactory dynamicChatModelFactory;

    public List<ProviderCatalogResponse> listProviderCatalog() {
        return Arrays.stream(ModelProviderEnum.values())
                .map(item -> ProviderCatalogResponse.builder()
                        .providerEnum(item.name())
                        .providerLabel(item.name())
                        .build())
                .toList();
    }

    public List<ModelProviderConfigResponse> listProviderConfigs() {
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        return modelProviderConfigService.listByTenantId(tenantId).stream()
                .map(this::toProviderResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelProviderConfigResponse createProviderConfig(ModelProviderConfigSaveRequest request) {
        validateProviderRequest(request, true);
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        Long currentUserId = currentUserContextSupport.getCurrentUserId();
        String currentUserName = currentUserContextSupport.getCurrentUserName();
        validateProviderNameUnique(tenantId, request.getProviderName(), null);

        ModelProviderConfig entity = new ModelProviderConfig();
        entity.setProviderConfigCode(UUID.randomUUID().toString());
        entity.setProviderEnum(normalizeProviderEnum(request.getProviderEnum()));
        entity.setProviderName(request.getProviderName().trim());
        entity.setBaseUrl(trimToNull(request.getBaseUrl()));
        entity.setApiKeyCipherText(modelSecretCryptoService.encrypt(request.getApiKey()));
        entity.setOrganizationId(trimToNull(request.getOrganizationId()));
        entity.setDefaultHeadersJson(normalizeJson(request.getDefaultHeadersJson(), "defaultHeadersJson"));
        entity.setRemark(trimToNull(request.getRemark()));
        entity.setStatus(defaultStatus(request.getStatus()));
        entity.setTenantId(tenantId);
        entity.setOwnerUserId(currentUserId);
        entity.setOwnerUserName(currentUserName);
        modelProviderConfigService.save(entity);
        return toProviderResponse(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelProviderConfigResponse updateProviderConfig(String providerConfigCode, ModelProviderConfigSaveRequest request) {
        validateProviderRequest(request, false);
        ModelProviderConfig entity = requireProvider(providerConfigCode);
        validateProviderNameUnique(entity.getTenantId(), request.getProviderName(), entity.getId());

        entity.setProviderEnum(normalizeProviderEnum(request.getProviderEnum()));
        entity.setProviderName(request.getProviderName().trim());
        entity.setBaseUrl(trimToNull(request.getBaseUrl()));
        if (StringUtils.hasText(request.getApiKey())) {
            entity.setApiKeyCipherText(modelSecretCryptoService.encrypt(request.getApiKey()));
        }
        entity.setOrganizationId(trimToNull(request.getOrganizationId()));
        entity.setDefaultHeadersJson(normalizeJson(request.getDefaultHeadersJson(), "defaultHeadersJson"));
        entity.setRemark(trimToNull(request.getRemark()));
        entity.setStatus(defaultStatus(request.getStatus()));
        modelProviderConfigService.updateById(entity);
        return toProviderResponse(entity);
    }

    public List<ModelDefinitionResponse> listModels(Boolean enabledOnly) {
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        List<ModelDefinition> entities = Boolean.TRUE.equals(enabledOnly)
                ? modelDefinitionService.listEnabledByTenantId(tenantId)
                : modelDefinitionService.listByTenantId(tenantId);
        return entities.stream().map(this::toModelResponse).toList();
    }

    public List<ModelOptionResponse> listEnabledModelOptions() {
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        return modelDefinitionService.listEnabledByTenantId(tenantId).stream()
                .map(this::toModelOption)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelDefinitionResponse createModel(ModelDefinitionSaveRequest request) {
        validateModelRequest(request);
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        Long currentUserId = currentUserContextSupport.getCurrentUserId();
        String currentUserName = currentUserContextSupport.getCurrentUserName();
        ModelProviderConfig provider = requireEnabledProvider(request.getProviderConfigCode());
        validateModelNameUnique(tenantId, request.getModelName(), null);

        ModelDefinition entity = buildModelEntity(request, provider);
        entity.setModelCode(UUID.randomUUID().toString());
        entity.setTenantId(tenantId);
        entity.setOwnerUserId(currentUserId);
        entity.setOwnerUserName(currentUserName);
        modelDefinitionService.save(entity);
        resetOtherDefaultModels(entity);
        return toModelResponse(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelDefinitionResponse updateModel(String modelCode, ModelDefinitionSaveRequest request) {
        validateModelRequest(request);
        ModelDefinition entity = requireModel(modelCode);
        ModelProviderConfig provider = requireEnabledProvider(request.getProviderConfigCode());
        validateModelNameUnique(entity.getTenantId(), request.getModelName(), entity.getId());

        ModelDefinition source = buildModelEntity(request, provider);
        entity.setModelName(source.getModelName());
        entity.setProviderConfigId(source.getProviderConfigId());
        entity.setProviderConfigCode(source.getProviderConfigCode());
        entity.setProviderEnum(source.getProviderEnum());
        entity.setModelType(source.getModelType());
        entity.setModelIdentifier(source.getModelIdentifier());
        entity.setTemperature(source.getTemperature());
        entity.setTopP(source.getTopP());
        entity.setPresencePenalty(source.getPresencePenalty());
        entity.setFrequencyPenalty(source.getFrequencyPenalty());
        entity.setMaxTokens(source.getMaxTokens());
        entity.setContextWindow(source.getContextWindow());
        entity.setRpmLimit(source.getRpmLimit());
        entity.setTpmLimit(source.getTpmLimit());
        entity.setTimeoutMs(source.getTimeoutMs());
        entity.setSupportStreaming(source.getSupportStreaming());
        entity.setSupportTools(source.getSupportTools());
        entity.setSupportVision(source.getSupportVision());
        entity.setSupportJsonSchema(source.getSupportJsonSchema());
        entity.setIsDefault(source.getIsDefault());
        entity.setStatus(source.getStatus());
        entity.setAdvancedConfigJson(source.getAdvancedConfigJson());
        entity.setRemark(source.getRemark());
        modelDefinitionService.updateById(entity);
        resetOtherDefaultModels(entity);
        return toModelResponse(entity);
    }

    public ModelDefinition requireEnabledModelByCode(String modelCode) {
        ModelDefinition entity = requireModel(modelCode);
        if (!"ENABLED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "当前模型未启用");
        }
        return entity;
    }

    public ModelOptionResponse getEnabledModelOption(String modelCode) {
        return toModelOption(requireEnabledModelByCode(modelCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProviderConfig(String providerConfigCode) {
        ModelProviderConfig provider = requireProvider(providerConfigCode);
        long modelCount = modelDefinitionService.count(Wrappers.lambdaQuery(ModelDefinition.class)
                .eq(ModelDefinition::getTenantId, provider.getTenantId())
                .eq(ModelDefinition::getProviderConfigId, provider.getId()));
        if (modelCount > 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "该提供商配置下仍存在模型，请先删除或迁移模型");
        }
        modelProviderConfigService.removeById(provider.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(String modelCode) {
        ModelDefinition model = requireModel(modelCode);
        long referenceCount = agentVersionService.count(Wrappers.lambdaQuery(AgentVersion.class)
                .eq(AgentVersion::getTenantId, model.getTenantId())
                .apply("config_snapshot_json like {0}", "%\\\"modelCode\\\":\\\"" + modelCode + "\\\"%"));
        if (referenceCount > 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "该模型已被 Agent 版本引用，不能直接删除");
        }
        modelDefinitionService.removeById(model.getId());
    }

    public ChatModel createChatModel(String modelCode) {
        ModelDefinition model = requireEnabledModelByCode(modelCode);
        ModelProviderConfig provider = requireEnabledProvider(model.getProviderConfigCode());
        ChatModelRequest request = new ChatModelRequest();
        request.setProvider(provider.getProviderEnum());
        request.setModel(model.getModelIdentifier());
        request.setApiKey(modelSecretCryptoService.decrypt(provider.getApiKeyCipherText()));
        request.setBaseUrl(provider.getBaseUrl());
        request.setOptions(toChatOptions(model));
        return dynamicChatModelFactory.create(request);
    }

    public ModelTestResponse testProviderConnection(ModelProviderTestRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "测试请求不能为空");
        }
        ModelProviderConfig storedProvider = StringUtils.hasText(request.getProviderConfigCode())
                ? requireProvider(request.getProviderConfigCode())
                : null;
        String providerEnum = storedProvider != null ? storedProvider.getProviderEnum() : request.getProviderEnum();
        String baseUrl = storedProvider != null ? storedProvider.getBaseUrl() : request.getBaseUrl();
        String apiKey = StringUtils.hasText(request.getApiKey())
                ? request.getApiKey().trim()
                : (storedProvider == null ? null : modelSecretCryptoService.decrypt(storedProvider.getApiKeyCipherText()));
        if (!StringUtils.hasText(providerEnum)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请选择模型提供商");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "API Key 不能为空");
        }
        String resolvedProviderEnum = normalizeProviderEnum(providerEnum);
        String testModelIdentifier = StringUtils.hasText(request.getTestModelIdentifier())
                ? request.getTestModelIdentifier().trim()
                : resolveDefaultTestModel(resolvedProviderEnum);
        ChatModelRequest modelRequest = new ChatModelRequest();
        modelRequest.setProvider(resolvedProviderEnum);
        modelRequest.setModel(testModelIdentifier);
        modelRequest.setApiKey(apiKey);
        modelRequest.setBaseUrl(trimToNull(baseUrl));
        ChatOptionsDTO options = new ChatOptionsDTO();
        options.setModel(testModelIdentifier);
        options.setTemperature(0.2D);
        options.setMaxTokens(128);
        modelRequest.setOptions(options);
        return doTest(modelRequest, defaultPrompt(request.getTestPrompt()));
    }

    public ModelTestResponse testModel(String modelCode, ModelTestRequest request) {
        ModelDefinition model = requireEnabledModelByCode(modelCode);
        ModelProviderConfig provider = requireEnabledProvider(model.getProviderConfigCode());
        ChatModelRequest modelRequest = new ChatModelRequest();
        modelRequest.setProvider(model.getProviderEnum());
        modelRequest.setModel(model.getModelIdentifier());
        modelRequest.setApiKey(modelSecretCryptoService.decrypt(provider.getApiKeyCipherText()));
        modelRequest.setBaseUrl(provider.getBaseUrl());
        modelRequest.setOptions(toChatOptions(model));
        return doTest(modelRequest, defaultPrompt(request == null ? null : request.getTestPrompt()));
    }

    private ModelDefinition buildModelEntity(ModelDefinitionSaveRequest request, ModelProviderConfig provider) {
        ModelDefinition entity = new ModelDefinition();
        entity.setModelName(request.getModelName().trim());
        entity.setProviderConfigId(provider.getId());
        entity.setProviderConfigCode(provider.getProviderConfigCode());
        entity.setProviderEnum(provider.getProviderEnum());
        entity.setModelType(defaultModelType(request.getModelType()));
        entity.setModelIdentifier(request.getModelIdentifier().trim());
        entity.setTemperature(normalizeDecimal(request.getTemperature(), 0D, 2D, "temperature"));
        entity.setTopP(normalizeDecimal(request.getTopP(), 0D, 1D, "topP"));
        entity.setPresencePenalty(normalizeDecimal(request.getPresencePenalty(), -2D, 2D, "presencePenalty"));
        entity.setFrequencyPenalty(normalizeDecimal(request.getFrequencyPenalty(), -2D, 2D, "frequencyPenalty"));
        entity.setMaxTokens(normalizeInteger(request.getMaxTokens(), 1, 1024 * 1024, "maxTokens"));
        entity.setContextWindow(normalizeInteger(request.getContextWindow(), 1, 10_000_000, "contextWindow"));
        entity.setRpmLimit(normalizeInteger(request.getRpmLimit(), 1, 10_000_000, "rpmLimit"));
        entity.setTpmLimit(normalizeInteger(request.getTpmLimit(), 1, 10_000_000, "tpmLimit"));
        entity.setTimeoutMs(normalizeInteger(request.getTimeoutMs(), 1000, 600_000, "timeoutMs"));
        entity.setSupportStreaming(toFlag(request.getSupportStreaming(), true));
        entity.setSupportTools(toFlag(request.getSupportTools(), true));
        entity.setSupportVision(toFlag(request.getSupportVision(), false));
        entity.setSupportJsonSchema(toFlag(request.getSupportJsonSchema(), false));
        entity.setIsDefault(toFlag(request.getDefaultModel(), false));
        entity.setStatus(defaultStatus(request.getStatus()));
        entity.setAdvancedConfigJson(normalizeJson(request.getAdvancedConfigJson(), "advancedConfigJson"));
        entity.setRemark(trimToNull(request.getRemark()));
        return entity;
    }

    private void validateProviderRequest(ModelProviderConfigSaveRequest request, boolean create) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型提供商配置不能为空");
        }
        if (!StringUtils.hasText(request.getProviderEnum())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请选择模型提供商");
        }
        if (!StringUtils.hasText(request.getProviderName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型提供商配置名称不能为空");
        }
        if (create && !StringUtils.hasText(request.getApiKey())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "API Key 不能为空");
        }
    }

    private void validateModelRequest(ModelDefinitionSaveRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型配置不能为空");
        }
        if (!StringUtils.hasText(request.getModelName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型名称不能为空");
        }
        if (!StringUtils.hasText(request.getProviderConfigCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请选择模型提供商配置");
        }
        if (!StringUtils.hasText(request.getModelIdentifier())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型标识不能为空");
        }
    }

    private void validateProviderNameUnique(Long tenantId, String providerName, Long excludeId) {
        long count = modelProviderConfigService.count(Wrappers.lambdaQuery(ModelProviderConfig.class)
                .eq(ModelProviderConfig::getTenantId, tenantId)
                .eq(ModelProviderConfig::getProviderName, providerName.trim())
                .ne(excludeId != null, ModelProviderConfig::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型提供商配置名称已存在");
        }
    }

    private void validateModelNameUnique(Long tenantId, String modelName, Long excludeId) {
        long count = modelDefinitionService.count(Wrappers.lambdaQuery(ModelDefinition.class)
                .eq(ModelDefinition::getTenantId, tenantId)
                .eq(ModelDefinition::getModelName, modelName.trim())
                .ne(excludeId != null, ModelDefinition::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型名称已存在");
        }
    }

    private void resetOtherDefaultModels(ModelDefinition currentModel) {
        if (!Integer.valueOf(1).equals(currentModel.getIsDefault())) {
            return;
        }
        modelDefinitionService.update(Wrappers.lambdaUpdate(ModelDefinition.class)
                .eq(ModelDefinition::getTenantId, currentModel.getTenantId())
                .eq(ModelDefinition::getProviderConfigId, currentModel.getProviderConfigId())
                .ne(ModelDefinition::getId, currentModel.getId())
                .set(ModelDefinition::getIsDefault, 0));
    }

    private ModelProviderConfig requireProvider(String providerConfigCode) {
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        ModelProviderConfig entity = modelProviderConfigService.getByProviderConfigCode(tenantId, providerConfigCode);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "未找到模型提供商配置");
        }
        return entity;
    }

    private ModelProviderConfig requireEnabledProvider(String providerConfigCode) {
        ModelProviderConfig entity = requireProvider(providerConfigCode);
        if (!"ENABLED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "当前模型提供商配置未启用");
        }
        if (!StringUtils.hasText(entity.getApiKeyCipherText())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "当前模型提供商未配置 API Key");
        }
        return entity;
    }

    private ModelDefinition requireModel(String modelCode) {
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        ModelDefinition entity = modelDefinitionService.getByModelCode(tenantId, modelCode);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "未找到模型配置");
        }
        return entity;
    }

    private ModelProviderConfigResponse toProviderResponse(ModelProviderConfig entity) {
        return ModelProviderConfigResponse.builder()
                .providerConfigCode(entity.getProviderConfigCode())
                .providerEnum(entity.getProviderEnum())
                .providerName(entity.getProviderName())
                .baseUrl(entity.getBaseUrl())
                .organizationId(entity.getOrganizationId())
                .defaultHeadersJson(entity.getDefaultHeadersJson())
                .status(entity.getStatus())
                .apiKeyMasked(maskSecret(entity.getApiKeyCipherText()))
                .apiKeyConfigured(StringUtils.hasText(entity.getApiKeyCipherText()))
                .ownerUserName(entity.getOwnerUserName())
                .updateTime(toEpochMilli(entity.getUpdateTime()))
                .remark(entity.getRemark())
                .build();
    }

    private ModelDefinitionResponse toModelResponse(ModelDefinition entity) {
        ModelProviderConfig provider = modelProviderConfigService.getById(entity.getProviderConfigId());
        return ModelDefinitionResponse.builder()
                .modelCode(entity.getModelCode())
                .modelName(entity.getModelName())
                .providerConfigCode(entity.getProviderConfigCode())
                .providerEnum(entity.getProviderEnum())
                .providerName(provider == null ? null : provider.getProviderName())
                .modelType(entity.getModelType())
                .modelIdentifier(entity.getModelIdentifier())
                .temperature(entity.getTemperature())
                .topP(entity.getTopP())
                .presencePenalty(entity.getPresencePenalty())
                .frequencyPenalty(entity.getFrequencyPenalty())
                .maxTokens(entity.getMaxTokens())
                .contextWindow(entity.getContextWindow())
                .rpmLimit(entity.getRpmLimit())
                .tpmLimit(entity.getTpmLimit())
                .timeoutMs(entity.getTimeoutMs())
                .supportStreaming(toBoolean(entity.getSupportStreaming()))
                .supportTools(toBoolean(entity.getSupportTools()))
                .supportVision(toBoolean(entity.getSupportVision()))
                .supportJsonSchema(toBoolean(entity.getSupportJsonSchema()))
                .defaultModel(toBoolean(entity.getIsDefault()))
                .status(entity.getStatus())
                .advancedConfigJson(entity.getAdvancedConfigJson())
                .remark(entity.getRemark())
                .updateTime(toEpochMilli(entity.getUpdateTime()))
                .build();
    }

    private ModelOptionResponse toModelOption(ModelDefinition entity) {
        ModelProviderConfig provider = modelProviderConfigService.getById(entity.getProviderConfigId());
        return ModelOptionResponse.builder()
                .modelCode(entity.getModelCode())
                .modelName(entity.getModelName())
                .providerConfigCode(entity.getProviderConfigCode())
                .providerEnum(entity.getProviderEnum())
                .providerName(provider == null ? null : provider.getProviderName())
                .modelIdentifier(entity.getModelIdentifier())
                .modelType(entity.getModelType())
                .defaultModel(toBoolean(entity.getIsDefault()))
                .build();
    }

    private String normalizeProviderEnum(String providerEnum) {
        return ModelProviderEnum.fromValue(providerEnum).name();
    }

    /**
     * 为供应商补齐默认测试模型，避免测试连接时必须手工输入。
     */
    private String resolveDefaultTestModel(String providerEnum) {
        return switch (providerEnum) {
            case "OPENAI" -> "gpt-4.1";
            case "DEEPSEEK" -> "deepseek-chat";
            case "DASHSCOPE" -> "qwen-max";
            case "ANTHROPIC" -> "claude-3-7-sonnet-latest";
            case "ZHIPU" -> "glm-4-plus";
            default -> throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "暂未配置该供应商的默认测试模型");
        };
    }

    private ChatOptionsDTO toChatOptions(ModelDefinition model) {
        ChatOptionsDTO options = new ChatOptionsDTO();
        options.setModel(model.getModelIdentifier());
        options.setTemperature(model.getTemperature());
        options.setTopP(model.getTopP());
        options.setPresencePenalty(model.getPresencePenalty());
        options.setFrequencyPenalty(model.getFrequencyPenalty());
        options.setMaxTokens(model.getMaxTokens());
        return options;
    }

    private ModelTestResponse doTest(ChatModelRequest request, String prompt) {
        long start = System.currentTimeMillis();
        ChatClient chatClient = dynamicChatModelFactory.createChatClient(request);
        String content = chatClient.prompt(prompt).call().content();
        return ModelTestResponse.builder()
                .success(Boolean.TRUE)
                .providerEnum(request.getProvider())
                .modelIdentifier(request.getModel())
                .responseContent(content)
                .elapsedMs(System.currentTimeMillis() - start)
                .build();
    }

    private String defaultPrompt(String prompt) {
        return StringUtils.hasText(prompt) ? prompt.trim() : "请只回复：MODEL_OK";
    }

    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : "ENABLED";
    }

    private String defaultModelType(String modelType) {
        return StringUtils.hasText(modelType) ? modelType.trim().toUpperCase() : "CHAT";
    }

    private Integer toFlag(Boolean value, boolean defaultValue) {
        boolean resolved = value == null ? defaultValue : value;
        return resolved ? 1 : 0;
    }

    private Boolean toBoolean(Integer value) {
        return Integer.valueOf(1).equals(value);
    }

    private Double normalizeDecimal(Double value, Double min, Double max, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value < min || value > max) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, fieldName + " 超出允许范围");
        }
        return value;
    }

    private Integer normalizeInteger(Integer value, Integer min, Integer max, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value < min || value > max) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, fieldName + " 超出允许范围");
        }
        return value;
    }

    private String normalizeJson(String rawJson, String fieldName) {
        String value = trimToNull(rawJson);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(value));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, fieldName + " 不是合法 JSON");
        }
    }

    private String maskSecret(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        String plainText = modelSecretCryptoService.decrypt(cipherText);
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        int visible = Math.min(4, plainText.length());
        return "*".repeat(Math.max(0, plainText.length() - visible)) + plainText.substring(plainText.length() - visible);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long toEpochMilli(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
