package com.spring.ai.core.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.core.application.manager.CoreApplicationManager;
import com.spring.ai.core.application.service.ImageProxyService;
import com.spring.ai.core.domain.request.ImageGenerationProxyRequest;
import com.spring.ai.core.domain.request.ModelConnectionSaveRequest;
import com.spring.ai.core.domain.request.ModelConnectionTestRequest;
import com.spring.ai.core.domain.response.ModelConnectionResponse;
import com.spring.ai.core.domain.response.ModelOptionResponse;
import com.spring.ai.core.domain.response.ModelTestResponse;
import com.spring.ai.core.domain.response.ProviderCatalogResponse;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 核心模块控制器。
 * 负责暴露模型连接配置与模型选项相关接口，
 * 控制器仅做请求接收与响应包装，具体业务逻辑统一下沉到应用层。
 */
@RestController
@RequestMapping("/core")
public class CoreController {

    @Resource
    private CoreApplicationManager coreApplicationManager;

    @Resource
    private ImageProxyService imageProxyService;

    /**
     * 查询系统支持的模型提供商目录。
     */
    @GetMapping("/provider-catalog")
    public ApiResponse<List<ProviderCatalogResponse>> listProviderCatalog() {
        return ApiResponse.success(coreApplicationManager.listProviderCatalog());
    }

    @GetMapping("/model-connections")
    public ApiResponse<List<ModelConnectionResponse>> listModelConnections() {
        return ApiResponse.success(coreApplicationManager.listModelConnections());
    }

    @PostMapping("/model-connections")
    public ApiResponse<ModelConnectionResponse> saveModelConnection(@RequestBody ModelConnectionSaveRequest request) {
        return ApiResponse.success(coreApplicationManager.saveModelConnection(request));
    }

    @DeleteMapping("/model-connections/{modelCode}")
    public ApiResponse<Void> deleteModelConnection(@PathVariable("modelCode") String modelCode) {
        coreApplicationManager.deleteModelConnection(modelCode);
        return ApiResponse.success(null);
    }

    @PostMapping("/model-connections/test")
    public ApiResponse<ModelTestResponse> testModelConnection(@RequestBody ModelConnectionTestRequest request) {
        return ApiResponse.success(coreApplicationManager.testModelConnection(request));
    }

    /**
     * 查询可直接用于业务选择的启用模型选项列表。
     */
    @GetMapping("/models/options")
    public ApiResponse<List<ModelOptionResponse>> listEnabledModelOptions() {
        return ApiResponse.success(coreApplicationManager.listEnabledModelOptions());
    }

    /**
     * 代理图片生成请求，避免浏览器直接访问第三方域名时被 CORS 拦截。
     */
    @PostMapping("/image-proxy/generations")
    public ResponseEntity<String> proxyImageGeneration(@RequestBody ImageGenerationProxyRequest request) {
        return imageProxyService.proxyGeneration(request);
    }

    /**
     * 代理图片编辑请求，统一由后端转发 multipart/form-data 到上游接口。
     */
    @PostMapping(value = "/image-proxy/edits", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> proxyImageEdit(@RequestParam("baseUrl") String baseUrl,
                                                 @RequestParam("apiKey") String apiKey,
                                                 @RequestParam(value = "endpointPath", required = false) String endpointPath,
                                                 @RequestParam Map<String, String> formFields,
                                                 @RequestParam("image[]") MultipartFile[] imageFiles,
                                                 @RequestParam(value = "mask", required = false) MultipartFile maskFile) {
        return imageProxyService.proxyEdit(baseUrl, apiKey, endpointPath, formFields, imageFiles, maskFile);
    }
}
