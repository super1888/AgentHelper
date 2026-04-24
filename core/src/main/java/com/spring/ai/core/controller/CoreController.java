package com.spring.ai.core.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.core.application.manager.CoreApplicationManager;
import com.spring.ai.core.domain.request.ModelDefinitionSaveRequest;
import com.spring.ai.core.domain.request.ModelProviderConfigSaveRequest;
import com.spring.ai.core.domain.request.ModelProviderTestRequest;
import com.spring.ai.core.domain.request.ModelTestRequest;
import com.spring.ai.core.domain.response.ModelDefinitionResponse;
import com.spring.ai.core.domain.response.ModelOptionResponse;
import com.spring.ai.core.domain.response.ModelProviderConfigResponse;
import com.spring.ai.core.domain.response.ModelTestResponse;
import com.spring.ai.core.domain.response.ProviderCatalogResponse;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 核心模块控制器。
 * 负责暴露模型提供商配置、模型配置以及连通性测试相关接口，
 * 控制器仅做请求接收与响应包装，具体业务逻辑统一下沉到应用层。
 */
@RestController
@RequestMapping("/core")
public class CoreController {

    @Resource
    private CoreApplicationManager coreApplicationManager;

    /**
     * 查询系统支持的模型提供商目录。
     */
    @GetMapping("/provider-catalog")
    public ApiResponse<List<ProviderCatalogResponse>> listProviderCatalog() {
        return ApiResponse.success(coreApplicationManager.listProviderCatalog());
    }

    /**
     * 查询当前租户下已配置的模型提供商列表。
     */
    @GetMapping("/model-providers")
    public ApiResponse<List<ModelProviderConfigResponse>> listProviderConfigs() {
        return ApiResponse.success(coreApplicationManager.listProviderConfigs());
    }

    /**
     * 新增模型提供商配置。
     */
    @PostMapping("/model-providers")
    public ApiResponse<ModelProviderConfigResponse> createProviderConfig(@RequestBody ModelProviderConfigSaveRequest request) {
        return ApiResponse.success(coreApplicationManager.createProviderConfig(request));
    }

    /**
     * 更新指定的模型提供商配置。
     */
    @PatchMapping("/model-providers/{providerConfigCode}")
    public ApiResponse<ModelProviderConfigResponse> updateProviderConfig(
            @PathVariable("providerConfigCode") String providerConfigCode,
            @RequestBody ModelProviderConfigSaveRequest request
    ) {
        return ApiResponse.success(coreApplicationManager.updateProviderConfig(providerConfigCode, request));
    }

    /**
     * 删除模型提供商配置。
     */
    @DeleteMapping("/model-providers/{providerConfigCode}")
    public ApiResponse<Void> deleteProviderConfig(@PathVariable("providerConfigCode") String providerConfigCode) {
        coreApplicationManager.deleteProviderConfig(providerConfigCode);
        return ApiResponse.success(null);
    }

    /**
     * 测试模型提供商配置是否可用。
     */
    @PostMapping("/model-providers/test")
    public ApiResponse<ModelTestResponse> testProviderConnection(@RequestBody ModelProviderTestRequest request) {
        return ApiResponse.success(coreApplicationManager.testProviderConnection(request));
    }

    /**
     * 查询模型配置列表。
     * enabledOnly 为 true 时仅返回启用中的模型。
     */
    @GetMapping("/models")
    public ApiResponse<List<ModelDefinitionResponse>> listModels(
            @RequestParam(value = "enabledOnly", required = false) Boolean enabledOnly
    ) {
        return ApiResponse.success(coreApplicationManager.listModels(enabledOnly));
    }

    /**
     * 查询可直接用于业务选择的启用模型选项列表。
     */
    @GetMapping("/models/options")
    public ApiResponse<List<ModelOptionResponse>> listEnabledModelOptions() {
        return ApiResponse.success(coreApplicationManager.listEnabledModelOptions());
    }

    /**
     * 新增模型配置。
     */
    @PostMapping("/models")
    public ApiResponse<ModelDefinitionResponse> createModel(@RequestBody ModelDefinitionSaveRequest request) {
        return ApiResponse.success(coreApplicationManager.createModel(request));
    }

    /**
     * 更新模型配置。
     */
    @PatchMapping("/models/{modelCode}")
    public ApiResponse<ModelDefinitionResponse> updateModel(
            @PathVariable("modelCode") String modelCode,
            @RequestBody ModelDefinitionSaveRequest request
    ) {
        return ApiResponse.success(coreApplicationManager.updateModel(modelCode, request));
    }

    /**
     * 删除模型配置。
     */
    @DeleteMapping("/models/{modelCode}")
    public ApiResponse<Void> deleteModel(@PathVariable("modelCode") String modelCode) {
        coreApplicationManager.deleteModel(modelCode);
        return ApiResponse.success(null);
    }

    /**
     * 测试指定模型配置的实际调用结果。
     */
    @PostMapping("/models/{modelCode}/test")
    public ApiResponse<ModelTestResponse> testModel(
            @PathVariable("modelCode") String modelCode,
            @RequestBody(required = false) ModelTestRequest request
    ) {
        return ApiResponse.success(coreApplicationManager.testModel(modelCode, request));
    }
}
