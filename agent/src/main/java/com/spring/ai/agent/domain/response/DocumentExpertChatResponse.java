package com.spring.ai.agent.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 文档专家 Agent 对话响应。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Data
@Builder
public class DocumentExpertChatResponse {

    /**
     * 实际使用的模型编码。
     */
    private String modelCode;

    /**
     * 原始用户输入。
     */
    private String userPrompt;

    /**
     * 是否需要用户补充信息。
     */
    private Boolean clarificationNeeded;

    /**
     * 需要补充时返回的问题提示。
     */
    private String clarificationQuestion;

    /**
     * 路由与准入层结果。
     */
    private StageResult routeStage;

    /**
     * 提示词增强层结果。
     */
    private StageResult enhancementStage;

    /**
     * 生成 Agent A 输出。
     */
    private StageResult generationStageA;

    /**
     * 生成 Agent B 输出。
     */
    private StageResult generationStageB;

    /**
     * 审核层结果。
     */
    private StageResult auditStage;

    /**
     * 融合层结果。
     */
    private StageResult fusionStage;

    /**
     * 最终成稿。
     */
    private String finalDocument;

    /**
     * 汇总警告信息。
     */
    private List<String> warnings;

    @Data
    @Builder
    public static class StageResult {

        /**
         * 当前阶段名称。
         */
        private String stageName;

        /**
         * 当前阶段状态。
         */
        private String status;

        /**
         * 当前阶段实际使用的模型编码。
         */
        private String modelCode;

        /**
         * 阶段摘要。
         */
        private String summary;

        /**
         * 阶段输出文本。
         */
        private String content;

        /**
         * 阶段问题列表。
         */
        private List<String> issues;
    }
}
