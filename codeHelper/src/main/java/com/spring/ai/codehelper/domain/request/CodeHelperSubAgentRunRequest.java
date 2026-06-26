package com.spring.ai.codehelper.domain.request;

import java.util.List;
import lombok.Data;

/**
 * codeHelper 子 Agent 执行请求。
 *
 * @author zhouqi
 * @since 2026/6/26
 */
@Data
public class CodeHelperSubAgentRunRequest {

    /**
     * 子 Agent 类型。不传时由调度器根据任务内容自动选择。
     */
    private String agentType;

    /**
     * 子 Agent 要完成的具体任务。
     */
    private String task;

    /**
     * 使用的模型编码。不传时复用会话默认模型。
     */
    private String modelCode;

    /**
     * 是否允许自动执行低风险工具。
     */
    private Boolean autoToolCall;

    /**
     * 允许的工具白名单。不传时使用角色默认工具。
     */
    private List<String> allowedTools;
}
