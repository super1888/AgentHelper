package com.spring.ai.vectorstore.controller;

import com.spring.ai.vectorstore.domain.response.VectorStoreDeleteResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreFileListResponse;
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
 * 文件用途：向量存储管理控制器
 * 作者：Codex
 * 创建时间：2026-04-16
 * 核心功能：对外暴露向量文件上传、文件列表查询、语义检索与删除接口。
 */
@Tag(name = "向量存储管理")
@RestController
@RequestMapping("/vectorStore")
public class VectorStoreController {

    @Resource
    private VectorStoreService vectorStoreService;

    /**
     * 上传文件并写入向量库。
     *
     * @param file 上传文件
     * @return 上传结果
     */
    @Operation(summary = "上传文件到向量库")
    @PostMapping("/upload")
    public VectorStoreUploadResponse upload(@RequestParam("file") MultipartFile file) {
        return vectorStoreService.upload(file);
    }

    /**
     * 查询当前模块下已入库文件的汇总信息。
     *
     * @return 文件汇总列表
     */
    @Operation(summary = "查询向量存储文件列表")
    @GetMapping("/files")
    public VectorStoreFileListResponse listFiles() {
        return vectorStoreService.listFiles();
    }

    /**
     * 按检索词检索向量库内容。
     *
     * @param query 检索问题
     * @param fileName 文件名过滤条件
     * @param topK 返回条数
     * @param similarityThreshold 相似度阈值
     * @return 检索结果
     */
    @Operation(summary = "检索向量存储内容")
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
     * 清空当前模块全部向量数据。
     *
     * @return 删除结果
     */
    @Operation(summary = "清空当前模块向量数据")
    @PostMapping("/deleteAll")
    public VectorStoreDeleteResponse deleteAll() {
        return vectorStoreService.deleteAll();
    }

    /**
     * 按文件名删除向量数据。
     *
     * @param fileName 文件名
     * @return 删除结果
     */
    @Operation(summary = "按文件名删除向量数据")
    @PostMapping("/deleteByFileName")
    public VectorStoreDeleteResponse deleteByFileName(@RequestParam String fileName) {
        return vectorStoreService.deleteByFileName(fileName);
    }
}
