package com.spring.ai.agent.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档专家路由与准入阶段 DTO。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessDecisionDTO {

    private Boolean allowed;

    private String reason;

    private String normalizedIntent;

    private List<String> issues;
}
