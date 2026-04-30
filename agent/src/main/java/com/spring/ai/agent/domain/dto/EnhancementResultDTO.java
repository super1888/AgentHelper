package com.spring.ai.agent.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档专家提示词增强阶段 DTO。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnhancementResultDTO {

    private String documentType;

    private String scenario;

    private String audience;

    private String tone;

    private List<String> structureOutline;

    private String wordCountRequirement;

    private String styleRequirement;

    private List<String> keyPoints;

    private Boolean needClarification;

    private String clarificationQuestion;

    private List<String> missingItems;

    private String summary;

    private String structuredInstruction;
}
