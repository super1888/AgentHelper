package com.spring.ai.vectorstore.search;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

/**
 * 混合检索服务接口。
 *
 * <p>该接口定义统一的 RAG 召回入口，具体实现负责把向量检索、关键词检索、RRF 融合和 Rerank 串起来。</p>
 */
public interface HybridVectorSearchService {

    /**
     * 执行混合检索。
     *
     * @param searchRequest Spring AI 检索请求。query 是用户问题，topK 是最终返回数量，similarityThreshold 是向量相似度阈值，filterExpression 是模块或文件过滤条件。
     * @param fileName 文件名过滤条件。为空表示在当前模块全部文件中检索；非空表示关键词检索和文档枚举只处理该文件。
     * @return 最终命中文档列表。返回结果通常已经经过 RRF 融合和可选 Rerank，Document.score 表示最终排序分数。
     */
    List<Document> search(SearchRequest searchRequest, String fileName);
}