package com.spring.ai.vectorstore.controller;

import com.spring.ai.vectorstore.domain.response.VectorStoreDeleteResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreSearchResponse;
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

/**
 * 向量库文档管理控制器。
 */
@Tag(name = "向量库管理")
@RestController
@RequestMapping("/vectorStore")
public class VectorStoreController {

    @Resource
    VectorStoreService vectorStoreService;

    /**
     * 上传文件并写入向量库。
     */
    @Operation(summary = "上传文件到向量库")
    @PostMapping("/upload")
    public VectorStoreUploadResponse upload(@RequestParam("file") MultipartFile file) {
        return vectorStoreService.upload(file);
    }

    /**
     * 按查询词搜索向量库内容。
     */
    @Operation(summary = "检索向量库内容")
    @GetMapping("/search")
    public VectorStoreSearchResponse search(
            @RequestParam String query,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) Integer topK,
            @RequestParam(required = false) Double similarityThreshold
    ) {
        return vectorStoreService.search(query, fileName, topK, similarityThreshold);
    }

    /**
     * 删除当前模块全部向量数据。
     */
    @Operation(summary = "清空当前模块向量数据")
    @PostMapping("/deleteAll")
    public VectorStoreDeleteResponse deleteAll() {
        return vectorStoreService.deleteAll();
    }

    /**
     * 按文件名删除向量数据。
     */
    @Operation(summary = "按文件名删除向量数据")
    @PostMapping("/deleteByFileName")
    public VectorStoreDeleteResponse deleteByFileName(@RequestParam String fileName) {
        return vectorStoreService.deleteByFileName(fileName);
    }
}
