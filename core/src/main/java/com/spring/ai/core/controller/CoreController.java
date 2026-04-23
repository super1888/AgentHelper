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

@RestController
@RequestMapping("/core")
public class CoreController {

    @Resource
    private CoreApplicationManager coreApplicationManager;

    @GetMapping("/provider-catalog")
    public ApiResponse<List<ProviderCatalogResponse>> listProviderCatalog() {
        return ApiResponse.success(coreApplicationManager.listProviderCatalog());
    }

    @GetMapping("/model-providers")
    public ApiResponse<List<ModelProviderConfigResponse>> listProviderConfigs() {
        return ApiResponse.success(coreApplicationManager.listProviderConfigs());
    }

    @PostMapping("/model-providers")
    public ApiResponse<ModelProviderConfigResponse> createProviderConfig(@RequestBody ModelProviderConfigSaveRequest request) {
        return ApiResponse.success(coreApplicationManager.createProviderConfig(request));
    }

    @PatchMapping("/model-providers/{providerConfigCode}")
    public ApiResponse<ModelProviderConfigResponse> updateProviderConfig(
            @PathVariable("providerConfigCode") String providerConfigCode,
            @RequestBody ModelProviderConfigSaveRequest request
    ) {
        return ApiResponse.success(coreApplicationManager.updateProviderConfig(providerConfigCode, request));
    }

    @DeleteMapping("/model-providers/{providerConfigCode}")
    public ApiResponse<Void> deleteProviderConfig(@PathVariable("providerConfigCode") String providerConfigCode) {
        coreApplicationManager.deleteProviderConfig(providerConfigCode);
        return ApiResponse.success(null);
    }

    @PostMapping("/model-providers/test")
    public ApiResponse<ModelTestResponse> testProviderConnection(@RequestBody ModelProviderTestRequest request) {
        return ApiResponse.success(coreApplicationManager.testProviderConnection(request));
    }

    @GetMapping("/models")
    public ApiResponse<List<ModelDefinitionResponse>> listModels(
            @RequestParam(value = "enabledOnly", required = false) Boolean enabledOnly
    ) {
        return ApiResponse.success(coreApplicationManager.listModels(enabledOnly));
    }

    @GetMapping("/models/options")
    public ApiResponse<List<ModelOptionResponse>> listEnabledModelOptions() {
        return ApiResponse.success(coreApplicationManager.listEnabledModelOptions());
    }

    @PostMapping("/models")
    public ApiResponse<ModelDefinitionResponse> createModel(@RequestBody ModelDefinitionSaveRequest request) {
        return ApiResponse.success(coreApplicationManager.createModel(request));
    }

    @PatchMapping("/models/{modelCode}")
    public ApiResponse<ModelDefinitionResponse> updateModel(
            @PathVariable("modelCode") String modelCode,
            @RequestBody ModelDefinitionSaveRequest request
    ) {
        return ApiResponse.success(coreApplicationManager.updateModel(modelCode, request));
    }

    @DeleteMapping("/models/{modelCode}")
    public ApiResponse<Void> deleteModel(@PathVariable("modelCode") String modelCode) {
        coreApplicationManager.deleteModel(modelCode);
        return ApiResponse.success(null);
    }

    @PostMapping("/models/{modelCode}/test")
    public ApiResponse<ModelTestResponse> testModel(
            @PathVariable("modelCode") String modelCode,
            @RequestBody(required = false) ModelTestRequest request
    ) {
        return ApiResponse.success(coreApplicationManager.testModel(modelCode, request));
    }
}
