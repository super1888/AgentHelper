package com.spring.ai.agent.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档专家融合阶段 DTO。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FusionResultDTO {

    private String summary;

    private String finalDocument;
}
