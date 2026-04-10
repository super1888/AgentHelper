package com.spring.ai.common.constants;

/**
 * RAG 相关默认常量。
 *
 * <p>用于统一模块化 RAG 组件中的默认参数，避免硬编码散落在不同类中。</p>
 */
public final class RagConstants {

    /**
     * 默认相似度阈值。
     */
    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.50D;

    /**
     * 默认检索条数。
     */
    public static final int DEFAULT_TOP_K = 4;

    /**
     * 默认扩展查询数量。
     */
    public static final int DEFAULT_EXPANDED_QUERY_COUNT = 3;

    /**
     * 默认目标语言。
     */
    public static final String DEFAULT_TARGET_LANGUAGE = "english";

    /**
     * 默认目标检索系统描述。
     */
    public static final String DEFAULT_TARGET_SEARCH_SYSTEM = "vector store";

    /**
     * 默认文档截断长度。
     */
    public static final int DEFAULT_POST_PROCESSOR_MAX_CHARS = 800;

    private RagConstants() {
    }
}
