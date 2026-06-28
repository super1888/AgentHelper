package com.spring.ai.vectorstore.search;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

/**
 * 混合检索服务，统一执行向量检索、关键词检索、RRF 融合和重排。
 */
public interface HybridVectorSearchService {

    /**
     * 执行检索。
     * @param searchRequest Spring AI 向量检索请求
     * @param fileName 文件名过滤，可为空
     * @return 命中文档列表
     */
    List<Document> search(SearchRequest searchRequest, String fileName);
}
