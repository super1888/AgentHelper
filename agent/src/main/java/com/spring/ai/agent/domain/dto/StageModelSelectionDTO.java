package com.spring.ai.agent.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档专家各阶段模型选择 DTO。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageModelSelectionDTO {

    private String defaultModelCode;

    private String routeModelCode;

    private String enhancementModelCode;

    private String generationAModelCode;

    private String generationBModelCode;

    private String auditModelCode;

    private String fusionModelCode;
}
