package com.spring.ai.vectorstore.controller;

import com.spring.ai.vectorstore.dto.VectorStoreDeleteResponse;
import com.spring.ai.vectorstore.dto.VectorStoreSearchResponse;
import com.spring.ai.vectorstore.dto.VectorStoreUploadResponse;
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
 * 向量库文档管理控制器。 负责提供文档上传、检索、删除等对外接口。
 */
@Tag(name = "向量库管理")
@RestController
@RequestMapping("/vectorStore")
public class VectorStoreController {

    @Resource
    VectorStoreService vectorStoreService;

    /**
     * 上传文件并写入向量库。
     *
     * @param file 上传文件
     * @return 上传响应结果
     */
    @Operation(summary = "上传文件到向量库")
    @PostMapping("/upload")
    public VectorStoreUploadResponse upload(@RequestParam("file") MultipartFile file) {
        return vectorStoreService.upload(file);
    }

    /**
     * 根据查询词检索向量库中的文档切片。
     *
     * @param query               查询词
     * @param fileName            文件名过滤条件
     * @param topK                返回结果数
     * @param similarityThreshold 相似度阈值
     * @return 检索响应结果
     */
    @Operation(summary = "检索向量库内容")
    @GetMapping("/search")
    public VectorStoreSearchResponse search(
            @RequestParam String query,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) Integer topK,
            @RequestParam(required = false) Double similarityThreshold) {
        return vectorStoreService.search(query, fileName, topK, similarityThreshold);
    }

    /**
     * 删除当前模块写入的全部向量数据。
     *
     * @return 删除响应结果
     */
    @Operation(summary = "清空当前模块向量数据")
    @PostMapping("/deleteAll")
    public VectorStoreDeleteResponse deleteAll() {
        return vectorStoreService.deleteAll();
    }

    /**
     * 根据文件名删除对应的向量数据。
     *
     * @param fileName 文件名
     * @return 删除响应结果
     */
    @Operation(summary = "按文件名删除向量数据")
    @PostMapping("/deleteByFileName")
    public VectorStoreDeleteResponse deleteByFileName(@RequestParam String fileName) {
        return vectorStoreService.deleteByFileName(fileName);
    }
}
