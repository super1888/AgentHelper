package com.spring.ai.agent.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档专家审核阶段 DTO。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditResultDTO {

    private String summary;

    private List<String> issues;

    private Boolean hasSevereIssue;

    private String reviewedDocumentA;

    private String reviewedDocumentB;
}
