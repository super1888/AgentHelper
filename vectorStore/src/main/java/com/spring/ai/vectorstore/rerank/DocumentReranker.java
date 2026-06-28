package com.spring.ai.vectorstore.rerank;

import java.util.List;
import org.springframework.ai.document.Document;

/**
 * 检索重排器，用于对融合后的候选结果重新排序。
 */
public interface DocumentReranker {

    /**
     * 对候选文档进行重排。
     * @param query 检索词
     * @param candidates 候选文档
     * @return 重排后的文档
     */
    List<Document> rerank(String query, List<Document> candidates);
}
