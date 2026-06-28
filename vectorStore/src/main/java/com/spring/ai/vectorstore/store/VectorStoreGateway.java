package com.spring.ai.vectorstore.store;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter.Expression;

/**
 * 向量存储网关，屏蔽 Redis、Qdrant、FAISS 等后端差异。
 */
public interface VectorStoreGateway {

    /**
     * 检查当前向量库是否可用。
     */
    void ensureReady();

    /**
     * 批量写入文档。
     * @param documents 文档列表
     */
    void add(List<Document> documents);

    /**
     * 向量相似度检索。
     * @param searchRequest 检索请求
     * @return 命中文档
     */
    List<Document> similaritySearch(SearchRequest searchRequest);

    /**
     * 删除满足过滤条件的文档。
     * @param expression 过滤表达式
     */
    void delete(Expression expression);

    /**
     * 列出满足过滤条件的文档，用于关键词检索和管理页展示。
     * @param fileName 文件名过滤，可为空
     * @return 文档列表
     */
    List<Document> listDocuments(String fileName);
}
