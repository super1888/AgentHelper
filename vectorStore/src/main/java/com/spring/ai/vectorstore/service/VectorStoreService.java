package com.spring.ai.vectorstore.service;

import com.spring.ai.vectorstore.domain.response.VectorStoreDeleteResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreDocumentListResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreFileListResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreSearchResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreStatisticsResponse;
import com.spring.ai.vectorstore.domain.response.VectorStoreUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 向量存储服务接口
 */
public interface VectorStoreService {

    VectorStoreUploadResponse upload(MultipartFile file);

    VectorStoreUploadResponse importBigFile(String fileId);

    VectorStoreFileListResponse listFiles();

    VectorStoreDocumentListResponse listDocuments(String fileName);

    VectorStoreSearchResponse search(String query, String fileName, Integer topK, Double similarityThreshold);

    VectorStoreStatisticsResponse statistics();

    VectorStoreDeleteResponse deleteAll();

    VectorStoreDeleteResponse deleteByFileName(String fileName);
}
