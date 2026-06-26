package com.spring.ai.codehelper.domain.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * codeHelper 子 Agent 定义。
 * 用于描述子 Agent 的角色、边界和允许使用的工具集合。
 *
 * @author zhouqi
 * @since 2026/6/26
 */
@Data
@Builder
public class CodeHelperSubAgentDefinitionDTO {

    /**
     * 子 Agent 类型，例如 explorer、planner、coder、reviewer。
     */
    private String agentType;

    /**
     * 展示名称。
     */
    private String agentName;

    /**
     * 核心职责说明。
     */
    private String description;

    /**
     * 角色专属系统提示词。
     */
    private String systemPrompt;

    /**
     * 允许调用的工具名称。
     */
    private List<String> allowedTools;

    /**
     * 是否允许修改文件。
     */
    private Boolean writeAllowed;
}
