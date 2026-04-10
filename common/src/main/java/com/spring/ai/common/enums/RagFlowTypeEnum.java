package com.spring.ai.common.enums;

/**
 * RAG 流程类型枚举。
 */
public enum RagFlowTypeEnum {

    /**
     * 基础 QuestionAnswerAdvisor 流程。
     */
    QUESTION_ANSWER_ADVISOR,

    /**
     * 模块化 RetrievalAugmentationAdvisor 流程。
     */
    RETRIEVAL_AUGMENTATION_ADVISOR,

    /**
     * 手动编排模块化 RAG 流程。
     */
    MANUAL_MODULAR_RAG
}
