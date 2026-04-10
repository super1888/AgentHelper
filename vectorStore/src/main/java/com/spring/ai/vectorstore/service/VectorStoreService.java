package com.spring.ai.vectorstore.service;

import com.spring.ai.vectorstore.domain.response.VectorStoreDeleteResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreSearchResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 向量库服务接口。
 * 统一定义文档入库、检索和删除能力。
 */
public interface VectorStoreService {

    /**
     * 上传文件并写入向量库。
     *
     * @param file 上传文件
     * @return 上传结果
     */
    VectorStoreUploadResponse upload(MultipartFile file);

    /**
     * 检索向量库。
     *
     * @param query 查询词
     * @param fileName 文件名过滤条件
     * @param topK 返回数量
     * @param similarityThreshold 相似度阈值
     * @return 检索结果
     */
    VectorStoreSearchResponse search(String query, String fileName, Integer topK, Double similarityThreshold);

    /**
     * 删除当前模块全部向量数据。
     *
     * @return 删除结果
     */
    VectorStoreDeleteResponse deleteAll();

    /**
     * 按文件名删除向量数据。
     *
     * @param fileName 文件名
     * @return 删除结果
     */
    VectorStoreDeleteResponse deleteByFileName(String fileName);
}
