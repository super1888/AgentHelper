package com.spring.ai.core.rag.domain.dto;

import com.spring.ai.common.enums.RagFlowTypeEnum;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.vectorstore.filter.Filter;

/**
 * 模块化 RAG 请求参数。
 *
 * <p>该 DTO 主要用于教学和演示：
 * 1. 可以显式控制每个 RAG 阶段是否启用
 * 2. 可以自由组合 QueryTransformer、QueryExpander、Retriever、PostProcessor、Augmenter
 * 3. 后续扩展新的参数时，只需要继续在这里补充即可</p>
 */
@Data
@Builder
public class ModularRagRequest {

    /**
     * 原始用户问题。
     */
    private String userQuery;

    /**
     * RAG 流程类型。
     */
    private RagFlowTypeEnum ragFlowType;

    /**
     * 是否启用查询重写。
     */
    private Boolean enableRewriteQuery;

    /**
     * 是否启用查询压缩。
     */
    private Boolean enableCompressionQuery;

    /**
     * 是否启用查询翻译。
     */
    private Boolean enableTranslationQuery;

    /**
     * 查询翻译目标语言。
     */
    private String targetLanguage;

    /**
     * 查询重写目标检索系统描述。
     */
    private String targetSearchSystem;

    /**
     * 是否启用多查询扩展。
     */
    private Boolean enableMultiQueryExpansion;

    /**
     * 扩展查询数量。
     */
    private Integer expandedQueryCount;

    /**
     * 检索 topK。
     */
    private Integer topK;

    /**
     * 相似度阈值。
     */
    private Double similarityThreshold;

    /**
     * 固定过滤表达式。
     */
    private Filter.Expression filterExpression;

    /**
     * 是否允许空上下文继续生成。
     */
    private Boolean allowEmptyContext;

    /**
     * 是否启用简单文档后处理器。
     */
    private Boolean enableSimpleDocumentPostProcessor;

    /**
     * 文档后处理器截断字符数。
     */
    private Integer postProcessorMaxChars;

    /**
     * 文档后处理关键字，仅保留命中文档。
     */
    private List<String> requiredKeywords;
}
