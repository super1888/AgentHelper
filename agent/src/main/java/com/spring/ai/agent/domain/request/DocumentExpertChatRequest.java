package com.spring.ai.agent.domain.request;

import lombok.Data;

/**
 * 文档专家 Agent 对话请求。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Data
public class DocumentExpertChatRequest {

    /**
     * 默认模型编码。
     * 当某个阶段未单独指定模型时，自动回退到该模型。
     */
    private String modelCode;

    /**
     * 路由与准入校验阶段模型编码。
     */
    private String routeModelCode;

    /**
     * 提示词增强阶段模型编码。
     */
    private String enhancementModelCode;

    /**
     * 双文档生成 A 阶段模型编码。
     */
    private String generationAModelCode;

    /**
     * 双文档生成 B 阶段模型编码。
     */
    private String generationBModelCode;

    /**
     * 文档审核阶段模型编码。
     */
    private String auditModelCode;

    /**
     * 融合汇总阶段模型编码。
     */
    private String fusionModelCode;

    /**
     * 用户原始输入。
     */
    private String userPrompt;

    /**
     * 是否允许在信息缺失时按默认规则自动补全。
     */
    private Boolean autoFillMissingInfo;
}
