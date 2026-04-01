package com.spring.ai.interceptors.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 在执行工具之前强制执行一个规划步骤，以概述 Agent 将要采取的步骤。
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@Data
@Builder
public class TodoListInterceptorDTO {

    /**
     * 任务规划系统提示词（告诉 LLM 如何使用 to.do 工具） 你提供的官方默认值（已完整保留）
     */
    private String systemPrompt;

    /**
     * to.do 工具描述（告诉 Agent 何时使用、如何使用、何时不使用） 你提供的官方默认值（已完整保留）
     */
    private String toolDescription;
}
