package com.spring.ai.interceptors.domain.dto;

import com.alibaba.cloud.ai.graph.agent.hook.TokenCounter;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * 在将上下文发送给 LLM 之前对其进行修改，以注入、删除或修改信息。
 * <p>
 * 适用场景：
 * <p>
 * 向 LLM 提供额外的上下文或指令； 从对话历史中删除不相关或冗余的信息； 动态修改上下文以引导 Agent 的行为。
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@Data
@Builder
public class ContextEditingInterceptorDTO {

    /**
     * 触发清理的 token 阈值 当上下文总 token 超过该值时，自动触发清理 默认：100000
     */
    private int trigger = 100000;

    /**
     * 每次至少清理的消息数量 默认：0（按需清理）
     */
    private int clearAtLeast = 0;

    /**
     * 保留最新的消息条数 永远保留最近 N 条消息不被清理 默认：3
     */
    private int keep = 3;

    /**
     * 是否清理工具调用的输入参数 true：清理工具入参，false：保留 默认：false
     */
    private boolean clearToolInputs = false;

    /**
     * 例外工具（不清理这些工具的调用记录） 工具名称集合
     */
    private Set<String> excludeTools = new HashSet<>();

    /**
     * 消息清理后替换的占位符 默认：[cleared]
     */
    private String placeholder = "[cleared]";

    /**
     * token 计算器 默认：approximateMsgCounter() 近似计数器
     */
    private TokenCounter tokenCounter = TokenCounter.approximateMsgCounter();

}
