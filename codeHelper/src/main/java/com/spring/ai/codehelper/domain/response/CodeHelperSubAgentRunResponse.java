package com.spring.ai.codehelper.domain.response;

import com.spring.ai.codehelper.domain.dto.CodeHelperSubAgentDefinitionDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperToolCallDTO;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * codeHelper 子 Agent 执行响应。
 *
 * @author zhouqi
 * @since 2026/6/26
 */
@Data
@Builder
public class CodeHelperSubAgentRunResponse {

    /**
     * 任务编号。
     */
    private String taskId;

    /**
     * 实际执行的子 Agent 定义。
     */
    private CodeHelperSubAgentDefinitionDTO agent;

    /**
     * 子 Agent 任务内容。
     */
    private String task;

    /**
     * 子 Agent 输出结论。
     */
    private String result;

    /**
     * 是否需要用户确认后继续执行。
     */
    private Boolean requireConfirmation;

    /**
     * 模型建议的工具调用。
     */
    private List<CodeHelperToolCallDTO> toolCalls;

    /**
     * 已执行工具的摘要。
     */
    private List<String> toolResults;

    /**
     * 调用耗时。
     */
    private Long costMs;
}
