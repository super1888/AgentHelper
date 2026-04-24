package com.spring.ai.core.application.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import com.spring.ai.common.utils.CommonJsonUtils;
import com.spring.ai.common.utils.CommonMaskingUtils;
import com.spring.ai.common.utils.CommonTextUtils;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.core.application.assembler.CoreAssembler;
import com.spring.ai.core.domain.request.ModelConnectionSaveRequest;
import com.spring.ai.core.domain.request.ModelConnectionTestRequest;
import com.spring.ai.core.domain.dto.ChatModelRequest;
import com.spring.ai.core.domain.dto.ChatOptionsDTO;
import com.spring.ai.core.domain.response.ModelConnectionResponse;
import com.spring.ai.core.domain.response.ModelOptionResponse;
import com.spring.ai.core.domain.response.ModelTestResponse;
import com.spring.ai.core.domain.response.ProviderCatalogResponse;
import com.spring.ai.core.facotry.DynamicChatModelFactory;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 核心模块应用层编排入口。 负责串联租户上下文、模型提供商配置、模型配置、密钥解密以及模型测试调用， 对外提供统一的模型管理能力。
 */
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
    private CommonJsonUtils commonJsonUtils;

    @Resource
    private DynamicChatModelFactory dynamicChatModelFactory;

    /**
     * 返回系统内置支持的模型提供商目录。
     */
    public List<ProviderCatalogResponse> listProviderCatalog() {
        return Arrays.stream(ModelProviderEnum.values())
                .map(item -> CoreAssembler.toProviderCatalogResponse(item.name()))
                .toList();
    }

    public List<ModelConnectionResponse> listModelConnections() {
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        Map<String, ModelProviderConfig> providerMap = modelProviderConfigService.listByTenantId(tenantId).stream()
                .collect(Collectors.toMap(ModelProviderConfig::getProviderConfigCode, Function.identity(), (left, right) -> left));
        return modelDefinitionService.listByTenantId(tenantId).stream()
                .map(model -> buildModelConnectionResponse(model, providerMap.get(model.getProviderConfigCode())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ModelConnectionResponse saveModelConnection(ModelConnectionSaveRequest request) {
        validateModelConnectionRequest(request);
        if (!StringUtils.hasText(request.getModelCode())) {
            return createModelConnection(request);
        }
        return updateModelConnection(request);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteModelConnection(String modelCode) {
        ModelDefinition model = requireModel(modelCode);
        ModelProviderConfig provider = requireProvider(model.getProviderConfigCode());
        deleteModel(modelCode);
        if (countModelsByProviderId(provider.getTenantId(), provider.getId()) == 0) {
            modelProviderConfigService.removeById(provider.getId());
        }
    }

    public ModelTestResponse testModelConnection(ModelConnectionTestRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型连接测试请求不能为空");
        }
        ModelDefinition storedModel = StringUtils.hasText(request.getModelCode())
                ? requireModel(request.getModelCode())
                : null;
        ModelProviderConfig storedProvider = storedModel == null ? null : requireProvider(storedModel.getProviderConfigCode());
        String providerEnum = StringUtils.hasText(request.getProviderEnum())
                ? request.getProviderEnum().trim()
                : (storedProvider == null ? null : storedProvider.getProviderEnum());
        String modelIdentifier = StringUtils.hasText(request.getModelIdentifier())
                ? request.getModelIdentifier().trim()
                : (storedModel == null ? null : storedModel.getModelIdentifier());
        String apiKey = StringUtils.hasText(request.getApiKey())
                ? request.getApiKey().trim()
                : (storedProvider == null ? null : modelSecretCryptoService.decrypt(storedProvider.getApiKeyCipherText()));
        if (!StringUtils.hasText(providerEnum)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请选择模型提供商");
        }
        if (!StringUtils.hasText(modelIdentifier)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型标识不能为空");
        }
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "API Key 不能为空");
        }
        ChatModelRequest modelRequest = new ChatModelRequest();
        modelRequest.setProvider(normalizeProviderEnum(providerEnum));
        modelRequest.setModel(modelIdentifier);
        modelRequest.setApiKey(apiKey);
        modelRequest.setBaseUrl(StringUtils.hasText(request.getBaseUrl())
                ? CommonTextUtils.trimToNull(request.getBaseUrl())
                : (storedProvider == null ? null : storedProvider.getBaseUrl()));
        ChatOptionsDTO options = storedModel == null ? new ChatOptionsDTO() : toChatOptions(storedModel);
        options.setModel(modelIdentifier);
        if (request.getTemperature() != null) {
            options.setTemperature(request.getTemperature());
        }
        if (request.getTopP() != null) {
            options.setTopP(request.getTopP());
        }
        if (request.getPresencePenalty() != null) {
            options.setPresencePenalty(request.getPresencePenalty());
        }
        if (request.getFrequencyPenalty() != null) {
            options.setFrequencyPenalty(request.getFrequencyPenalty());
        }
        if (request.getMaxTokens() != null) {
            options.setMaxTokens(request.getMaxTokens());
        }
        modelRequest.setOptions(options);
        return doTest(modelRequest, defaultPrompt(request.getTestPrompt()));
    }

    /**
     * 返回所有启用模型的轻量级选项信息。
     */
    public List<ModelOptionResponse> listEnabledModelOptions() {
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        Map<Long, ModelProviderConfig> providerMap = modelProviderConfigService.listByTenantId(tenantId).stream()
                .collect(Collectors.toMap(ModelProviderConfig::getId, Function.identity(), (left, right) -> left));
        return modelDefinitionService.listEnabledByTenantId(tenantId).stream()
                .map(model -> CoreAssembler.toModelOptionResponse(model, providerMap.get(model.getProviderConfigId())))
                .toList();
    }

    /**
     * 按编码获取启用中的模型配置。
     */
    public ModelDefinition requireEnabledModelByCode(String modelCode) {
        ModelDefinition entity = requireModel(modelCode);
        if (!"ENABLED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "当前模型未启用");
        }
        return entity;
    }

    /**
     * 获取指定模型的轻量级选项信息。
     */
    public ModelOptionResponse getEnabledModelOption(String modelCode) {
        ModelDefinition model = requireEnabledModelByCode(modelCode);
        ModelProviderConfig provider = modelProviderConfigService.getById(model.getProviderConfigId());
        return CoreAssembler.toModelOptionResponse(model, provider);
    }

    /**
     * 删除模型配置。 若模型已被 Agent 版本引用，则禁止直接删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(String modelCode) {
        ModelDefinition model = requireModel(modelCode);
        long referenceCount = agentVersionService.count(Wrappers.lambdaQuery(AgentVersion.class)
                .eq(AgentVersion::getTenantId, model.getTenantId())
                .apply("config_snapshot_json like {0}", "%\\\"modelCode\\\":\\\"" + modelCode + "\\\"%"));
        if (referenceCount > 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "该模型已被 Agent 版本引用，不能直接删除");
        }
        boolean removed = modelDefinitionService.remove(Wrappers.lambdaQuery(ModelDefinition.class)
                .eq(ModelDefinition::getTenantId, model.getTenantId())
                .eq(ModelDefinition::getModelCode, model.getModelCode()));
        if (!removed) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, HttpStatus.NOT_FOUND, "未找到模型配置或删除失败");
        }
    }

    /**
     * 基于数据库中的模型配置动态创建 ChatModel。
     */
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

    /**
     * 将前端提交的模型表单转换为数据库实体，并在构建过程中统一处理默认值与范围校验。
     */
    private ModelConnectionResponse createModelConnection(ModelConnectionSaveRequest request) {
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        Long currentUserId = currentUserContextSupport.getCurrentUserId();
        String currentUserName = currentUserContextSupport.getCurrentUserName();
        validateModelNameUnique(tenantId, request.getConnectionName(), null);

        ModelProviderConfig provider = buildProviderEntity(request, null);
        provider.setProviderConfigCode(UUID.randomUUID().toString());
        provider.setTenantId(tenantId);
        provider.setOwnerUserId(currentUserId);
        provider.setOwnerUserName(currentUserName);
        provider.setProviderName(generateProviderName(tenantId, request.getConnectionName(), null));
        modelProviderConfigService.save(provider);

        ModelDefinition model = buildModelEntity(request, provider, null);
        model.setModelCode(UUID.randomUUID().toString());
        model.setTenantId(tenantId);
        model.setOwnerUserId(currentUserId);
        model.setOwnerUserName(currentUserName);
        modelDefinitionService.save(model);
        resetOtherDefaultModels(model);
        return buildModelConnectionResponse(model, provider);
    }

    private ModelConnectionResponse updateModelConnection(ModelConnectionSaveRequest request) {
        ModelDefinition model = requireModel(request.getModelCode());
        ModelProviderConfig currentProvider = requireProvider(model.getProviderConfigCode());
        validateModelNameUnique(model.getTenantId(), request.getConnectionName(), model.getId());

        ModelProviderConfig targetProvider = currentProvider;
        boolean providerChanged = hasProviderConfigChanged(request, currentProvider);
        long providerReferenceCount = countModelsByProviderId(currentProvider.getTenantId(), currentProvider.getId());
        if (providerChanged) {
            if (providerReferenceCount > 1) {
                targetProvider = buildProviderEntity(request, null);
                if (!StringUtils.hasText(request.getApiKey())) {
                    targetProvider.setApiKeyCipherText(currentProvider.getApiKeyCipherText());
                }
                targetProvider.setProviderConfigCode(UUID.randomUUID().toString());
                targetProvider.setTenantId(currentProvider.getTenantId());
                targetProvider.setOwnerUserId(currentUserContextSupport.getCurrentUserId());
                targetProvider.setOwnerUserName(currentUserContextSupport.getCurrentUserName());
                targetProvider.setProviderName(generateProviderName(currentProvider.getTenantId(), request.getConnectionName(), null));
                modelProviderConfigService.save(targetProvider);
            } else {
                targetProvider = buildProviderEntity(request, currentProvider);
                targetProvider.setProviderName(
                        generateProviderName(currentProvider.getTenantId(), request.getConnectionName(), currentProvider.getId()));
                modelProviderConfigService.updateById(targetProvider);
            }
        }

        ModelDefinition source = buildModelEntity(request, targetProvider, model);
        applyModelEntity(model, source);
        modelDefinitionService.updateById(model);
        resetOtherDefaultModels(model);
        return buildModelConnectionResponse(model, targetProvider);
    }

    private ModelProviderConfig buildProviderEntity(ModelConnectionSaveRequest request, ModelProviderConfig existingProvider) {
        ModelProviderConfig entity = existingProvider == null ? new ModelProviderConfig() : existingProvider;
        entity.setProviderEnum(normalizeProviderEnum(request.getProviderEnum()));
        entity.setBaseUrl(CommonTextUtils.trimToNull(request.getBaseUrl()));
        if (existingProvider == null || StringUtils.hasText(request.getApiKey())) {
            entity.setApiKeyCipherText(modelSecretCryptoService.encrypt(request.getApiKey()));
        }
        entity.setOrganizationId(CommonTextUtils.trimToNull(request.getOrganizationId()));
        entity.setDefaultHeadersJson(commonJsonUtils.normalizeJsonOrNull(request.getDefaultHeadersJson(), "defaultHeadersJson"));
        entity.setRemark(CommonTextUtils.trimToNull(request.getRemark()));
        entity.setStatus(defaultStatus(request.getStatus()));
        return entity;
    }

    private ModelDefinition buildModelEntity(ModelConnectionSaveRequest request, ModelProviderConfig provider, ModelDefinition existingModel) {
        ModelDefinition entity = existingModel == null ? new ModelDefinition() : existingModel;
        entity.setModelName(request.getConnectionName().trim());
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
        entity.setAdvancedConfigJson(commonJsonUtils.normalizeJsonOrNull(request.getAdvancedConfigJson(), "advancedConfigJson"));
        entity.setRemark(CommonTextUtils.trimToNull(request.getRemark()));
        return entity;
    }

    private void applyModelEntity(ModelDefinition target, ModelDefinition source) {
        target.setModelName(source.getModelName());
        target.setProviderConfigId(source.getProviderConfigId());
        target.setProviderConfigCode(source.getProviderConfigCode());
        target.setProviderEnum(source.getProviderEnum());
        target.setModelType(source.getModelType());
        target.setModelIdentifier(source.getModelIdentifier());
        target.setTemperature(source.getTemperature());
        target.setTopP(source.getTopP());
        target.setPresencePenalty(source.getPresencePenalty());
        target.setFrequencyPenalty(source.getFrequencyPenalty());
        target.setMaxTokens(source.getMaxTokens());
        target.setContextWindow(source.getContextWindow());
        target.setRpmLimit(source.getRpmLimit());
        target.setTpmLimit(source.getTpmLimit());
        target.setTimeoutMs(source.getTimeoutMs());
        target.setSupportStreaming(source.getSupportStreaming());
        target.setSupportTools(source.getSupportTools());
        target.setSupportVision(source.getSupportVision());
        target.setSupportJsonSchema(source.getSupportJsonSchema());
        target.setIsDefault(source.getIsDefault());
        target.setStatus(source.getStatus());
        target.setAdvancedConfigJson(source.getAdvancedConfigJson());
        target.setRemark(source.getRemark());
    }

    /**
     * 校验模型配置请求中的核心字段。
     */
    private void validateModelConnectionRequest(ModelConnectionSaveRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型连接配置不能为空");
        }
        if (!StringUtils.hasText(request.getConnectionName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "连接配置名称不能为空");
        }
        if (!StringUtils.hasText(request.getProviderEnum())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请选择模型提供商");
        }
        if (!StringUtils.hasText(request.getModelIdentifier())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型标识不能为空");
        }
        if (!StringUtils.hasText(request.getModelCode()) && !StringUtils.hasText(request.getApiKey())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "首次创建时 API Key 不能为空");
        }
    }

    /**
     * 校验同一租户下的模型名称是否重复。
     */
    private void validateModelNameUnique(Long tenantId, String modelName, Long excludeId) {
        long count = modelDefinitionService.count(Wrappers.lambdaQuery(ModelDefinition.class)
                .eq(ModelDefinition::getTenantId, tenantId)
                .eq(ModelDefinition::getModelName, modelName.trim())
                .ne(excludeId != null, ModelDefinition::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "模型名称已存在");
        }
    }

    /**
     * 默认模型只允许在同一提供商配置下存在一个。 当当前模型被标记为默认时，需要将其他模型的默认标记清零。
     */
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

    /**
     * 根据配置编码获取模型提供商配置，并校验其归属当前租户。
     */
    private ModelProviderConfig requireProvider(String providerConfigCode) {
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        ModelProviderConfig entity = modelProviderConfigService.getByProviderConfigCode(tenantId, providerConfigCode);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "未找到模型提供商配置");
        }
        return entity;
    }

    /**
     * 获取启用中的模型提供商配置，并确保已经配置可用密钥。
     */
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

    /**
     * 根据模型编码获取当前租户下的模型配置。
     */
    private ModelDefinition requireModel(String modelCode) {
        Long tenantId = currentUserContextSupport.getCurrentTenantIdWithAutoInit();
        ModelDefinition entity = modelDefinitionService.getByModelCode(tenantId, modelCode);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "未找到模型配置");
        }
        return entity;
    }

    /**
     * 统一规范化模型提供商枚举值，兼容前端可能传入的不同大小写格式。
     */
    private String normalizeProviderEnum(String providerEnum) {
        return ModelProviderEnum.fromValue(providerEnum).name();
    }

    /**
     * 将模型配置中的采样参数整理为底层模型工厂可识别的选项对象。
     */
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

    /**
     * 执行一次真实模型调用，并返回耗时与响应内容。
     */
    private ModelTestResponse doTest(ChatModelRequest request, String prompt) {
        long start = System.currentTimeMillis();
        try {
            ChatClient chatClient = dynamicChatModelFactory.createChatClient(request);
            String content = chatClient.prompt(prompt).call().content();
            return CoreAssembler.toModelTestResponse(
                    request.getProvider(),
                    request.getModel(),
                    content,
                    System.currentTimeMillis() - start
            );
        } catch (Exception ex) {
            throw buildModelInvokeException(request, ex);
        }
    }

    /**
     * 当调用方未传入测试提示词时，使用统一的健康检查提示词。
     */
    private String defaultPrompt(String prompt) {
        return StringUtils.hasText(prompt) ? prompt.trim() : "请只回复：MODEL_OK";
    }

    /**
     * 默认状态为启用，避免前端遗漏状态字段时出现不可用脏数据。
     */
    private String defaultStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : "ENABLED";
    }

    /**
     * 默认模型类型为 CHAT。
     */
    private String defaultModelType(String modelType) {
        return StringUtils.hasText(modelType) ? modelType.trim().toUpperCase() : "CHAT";
    }

    /**
     * 将布尔值转换为数据库中使用的 0/1 标记。
     */
    private Integer toFlag(Boolean value, boolean defaultValue) {
        boolean resolved = value == null ? defaultValue : value;
        return resolved ? 1 : 0;
    }

    /**
     * 将数据库中的 0/1 标记还原为布尔值。
     */
    private Boolean toBoolean(Integer value) {
        return Integer.valueOf(1).equals(value);
    }

    /**
     * 校验并返回小数字段，统一处理上下限约束。
     */
    private Double normalizeDecimal(Double value, Double min, Double max, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value < min || value > max) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, fieldName + " 超出允许范围");
        }
        return value;
    }

    /**
     * 校验并返回整数字段，统一处理上下限约束。
     */
    private Integer normalizeInteger(Integer value, Integer min, Integer max, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value < min || value > max) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, fieldName + " 超出允许范围");
        }
        return value;
    }

    /**
     * 将底层模型调用异常包装为可读业务异常，便于前端直接展示问题原因。
     */
    private BusinessException buildModelInvokeException(ChatModelRequest request, Exception exception) {
        StringBuilder message = new StringBuilder("模型调用失败");
        if (StringUtils.hasText(request.getProvider())) {
            message.append("，provider=").append(request.getProvider());
        }
        if (StringUtils.hasText(request.getModel())) {
            message.append("，model=").append(request.getModel());
        }
        if (StringUtils.hasText(request.getBaseUrl())) {
            message.append("，baseUrl=").append(request.getBaseUrl());
        }
        String rootMessage = rootCauseMessage(exception);
        if (StringUtils.hasText(rootMessage)) {
            message.append("，原因：").append(rootMessage);
        }
        if (rootMessage != null && rootMessage.contains("404")) {
            message.append("。请检查模型供应商 baseUrl 是否填写为服务根地址，而不是 /chat/completions 等具体接口路径");
        }
        return new BusinessException(ErrorCodeEnum.BAD_REQUEST, HttpStatus.BAD_REQUEST, message.toString(), exception);
    }

    /**
     * 提取最底层异常消息，避免日志中只剩空白状态码。
     */
    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? null : CommonTextUtils.trimToNull(current.getMessage());
    }

    private long countModelsByProviderId(Long tenantId, Long providerId) {
        return modelDefinitionService.count(Wrappers.lambdaQuery(ModelDefinition.class)
                .eq(ModelDefinition::getTenantId, tenantId)
                .eq(ModelDefinition::getProviderConfigId, providerId));
    }

    private boolean hasProviderConfigChanged(ModelConnectionSaveRequest request, ModelProviderConfig provider) {
        if (!normalizeProviderEnum(request.getProviderEnum()).equals(provider.getProviderEnum())) {
            return true;
        }
        if (!CommonTextUtils.equalsNullableBlank(CommonTextUtils.trimToNull(request.getBaseUrl()), provider.getBaseUrl())) {
            return true;
        }
        if (!CommonTextUtils.equalsNullableBlank(CommonTextUtils.trimToNull(request.getOrganizationId()), provider.getOrganizationId())) {
            return true;
        }
        if (!CommonTextUtils.equalsNullableBlank(
                commonJsonUtils.normalizeJsonOrNull(request.getDefaultHeadersJson(), "defaultHeadersJson"),
                provider.getDefaultHeadersJson())) {
            return true;
        }
        if (!CommonTextUtils.equalsNullableBlank(defaultStatus(request.getStatus()), provider.getStatus())) {
            return true;
        }
        if (!CommonTextUtils.equalsNullableBlank(CommonTextUtils.trimToNull(request.getRemark()), provider.getRemark())) {
            return true;
        }
        return StringUtils.hasText(request.getApiKey());
    }

    private String generateProviderName(Long tenantId, String connectionName, Long excludeId) {
        String baseName = connectionName.trim() + "-provider";
        String candidate = baseName;
        int suffix = 2;
        while (modelProviderConfigService.count(Wrappers.lambdaQuery(ModelProviderConfig.class)
                .eq(ModelProviderConfig::getTenantId, tenantId)
                .eq(ModelProviderConfig::getProviderName, candidate)
                .ne(excludeId != null, ModelProviderConfig::getId, excludeId)) > 0) {
            candidate = baseName + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private ModelConnectionResponse buildModelConnectionResponse(ModelDefinition model, ModelProviderConfig provider) {
        String maskedApiKey = provider == null ? null : maskSecret(provider.getApiKeyCipherText());
        return CoreAssembler.toModelConnectionResponse(model, provider, maskedApiKey);
    }

    private String maskSecret(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        return CommonMaskingUtils.maskKeepTail(modelSecretCryptoService.decrypt(cipherText), 4);
    }
}
