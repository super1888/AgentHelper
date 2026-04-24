package com.spring.ai.core.application.assembler;

import com.spring.ai.common.utils.CommonTextUtils;
import com.spring.ai.common.repository.enitiy.ModelDefinition;
import com.spring.ai.common.repository.enitiy.ModelProviderConfig;
import com.spring.ai.core.domain.response.ModelConnectionResponse;
import com.spring.ai.core.domain.response.ModelOptionResponse;
import com.spring.ai.core.domain.response.ModelTestResponse;
import com.spring.ai.core.domain.response.ProviderCatalogResponse;

/**
 * 文件用途：Core 模块对象组装器
 * 核心职责：统一处理模型连接、模型选项与测试结果的响应组装
 */
public final class CoreAssembler {

    private CoreAssembler() {
    }

    public static ProviderCatalogResponse toProviderCatalogResponse(String providerEnum) {
        return ProviderCatalogResponse.builder()
                .providerEnum(providerEnum)
                .providerLabel(providerEnum)
                .build();
    }

    public static ModelOptionResponse toModelOptionResponse(ModelDefinition model, ModelProviderConfig provider) {
        return ModelOptionResponse.builder()
                .modelCode(model.getModelCode())
                .modelName(model.getModelName())
                .providerConfigCode(model.getProviderConfigCode())
                .providerEnum(model.getProviderEnum())
                .providerName(provider == null ? null : provider.getProviderName())
                .modelIdentifier(model.getModelIdentifier())
                .modelType(model.getModelType())
                .defaultModel(isEnabledFlag(model.getIsDefault()))
                .build();
    }

    public static ModelConnectionResponse toModelConnectionResponse(
            ModelDefinition model,
            ModelProviderConfig provider,
            String maskedApiKey
    ) {
        return ModelConnectionResponse.builder()
                .modelCode(model.getModelCode())
                .providerConfigCode(model.getProviderConfigCode())
                .connectionName(model.getModelName())
                .providerEnum(provider == null ? model.getProviderEnum() : provider.getProviderEnum())
                .baseUrl(provider == null ? null : provider.getBaseUrl())
                .organizationId(provider == null ? null : provider.getOrganizationId())
                .defaultHeadersJson(provider == null ? null : provider.getDefaultHeadersJson())
                .apiKeyMasked(maskedApiKey)
                .apiKeyConfigured(provider != null && isConfigured(provider.getApiKeyCipherText()))
                .modelType(model.getModelType())
                .modelIdentifier(model.getModelIdentifier())
                .temperature(model.getTemperature())
                .topP(model.getTopP())
                .presencePenalty(model.getPresencePenalty())
                .frequencyPenalty(model.getFrequencyPenalty())
                .maxTokens(model.getMaxTokens())
                .contextWindow(model.getContextWindow())
                .rpmLimit(model.getRpmLimit())
                .tpmLimit(model.getTpmLimit())
                .timeoutMs(model.getTimeoutMs())
                .supportStreaming(isEnabledFlag(model.getSupportStreaming()))
                .supportTools(isEnabledFlag(model.getSupportTools()))
                .supportVision(isEnabledFlag(model.getSupportVision()))
                .supportJsonSchema(isEnabledFlag(model.getSupportJsonSchema()))
                .defaultModel(isEnabledFlag(model.getIsDefault()))
                .advancedConfigJson(model.getAdvancedConfigJson())
                .status(model.getStatus())
                .remark(model.getRemark())
                .updateTime(CommonTextUtils.toEpochMilli(model.getUpdateTime()))
                .build();
    }

    public static ModelTestResponse toModelTestResponse(
            String providerEnum,
            String modelIdentifier,
            String responseContent,
            long elapsedMs
    ) {
        return ModelTestResponse.builder()
                .success(Boolean.TRUE)
                .providerEnum(providerEnum)
                .modelIdentifier(modelIdentifier)
                .responseContent(responseContent)
                .elapsedMs(elapsedMs)
                .build();
    }

    private static boolean isConfigured(String value) {
        return CommonTextUtils.trimToNull(value) != null;
    }

    private static Boolean isEnabledFlag(Integer value) {
        return Integer.valueOf(1).equals(value);
    }
}
