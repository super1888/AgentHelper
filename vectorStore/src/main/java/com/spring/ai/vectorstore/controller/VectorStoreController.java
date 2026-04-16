package com.spring.ai.vectorstore.controller;

import com.spring.ai.common.web.ApiResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreDeleteResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreDocumentListResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreFileListResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreSearchResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreStatisticsResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreUploadResponse;
import com.spring.ai.vectorstore.service.VectorStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "向量存储管理")
@RestController
@RequestMapping("/vectorStore")
public class VectorStoreController {

    @Resource
    private VectorStoreService vectorStoreService;

    @Operation(summary = "上传文件到向量库")
    @PostMapping("/upload")
    public ApiResponse<VectorStoreUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(vectorStoreService.upload(file));
    }

    @Operation(summary = "查询向量文件列表")
    @GetMapping("/files")
    public ApiResponse<VectorStoreFileListResponse> listFiles() {
        return ApiResponse.success(vectorStoreService.listFiles());
    }

    @Operation(summary = "查询向量文件切片列表")
    @GetMapping("/documents")
    public ApiResponse<VectorStoreDocumentListResponse> listDocuments(@RequestParam String fileName) {
        return ApiResponse.success(vectorStoreService.listDocuments(fileName));
    }

    @Operation(summary = "查询向量管理统计")
    @GetMapping("/statistics")
    public ApiResponse<VectorStoreStatisticsResponse> statistics() {
        return ApiResponse.success(vectorStoreService.statistics());
    }

    @Operation(summary = "检索向量内容")
    @GetMapping("/search")
    public ApiResponse<VectorStoreSearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) Integer topK,
            @RequestParam(required = false) Double similarityThreshold
    ) {
        return ApiResponse.success(vectorStoreService.search(query, fileName, topK, similarityThreshold));
    }

    @Operation(summary = "清空当前模块向量数据")
    @PostMapping("/deleteAll")
    public ApiResponse<VectorStoreDeleteResponse> deleteAll() {
        return ApiResponse.success(vectorStoreService.deleteAll());
    }

    @Operation(summary = "按文件名删除向量数据")
    @PostMapping("/deleteByFileName")
    public ApiResponse<VectorStoreDeleteResponse> deleteByFileName(@RequestParam String fileName) {
        return ApiResponse.success(vectorStoreService.deleteByFileName(fileName));
    }
}
