package com.spring.ai.core.rag.domain.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.document.Document;

/**
 * 模块化 RAG 执行结果。
 *
 * <p>该对象用于把模块化 RAG 每个阶段的中间结果都保留下来，方便学习和调试。</p>
 */
@Data
@Builder
public class ModularRagExecutionResult {

    /**
     * 原始查询。
     */
    private String originalQuery;

    /**
     * 经过查询转换后的查询。
     */
    private String transformedQuery;

    /**
     * 扩展后的查询列表。
     */
    private List<String> expandedQueries;

    /**
     * 检索并后处理后的文档列表。
     */
    private List<Document> documents;

    /**
     * 最终增强后的查询文本。
     */
    private String augmentedQuery;
}
