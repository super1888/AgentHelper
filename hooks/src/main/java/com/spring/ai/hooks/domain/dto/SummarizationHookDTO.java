package com.spring.ai.hooks.domain.dto;

import com.alibaba.cloud.ai.graph.agent.hook.TokenCounter;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 对话总结（Summarization）钩子配置参数 * 用于AI智能体对话历史过长时，自动压缩、总结历史记录
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@Builder
@Data
public class SummarizationHookDTO {

    /**
     * 对话模型（必选） 用于执行对话总结的AI大模型，如：通义千问、ChatGLM、DeepSeek等
     */
    private ChatModel chatModel;

    /**
     * 总结后的最大Token数 限制总结输出的长度，防止生成内容过长
     */
    private Integer maxTokens;

    /**
     * 触发总结的消息条数阈值 当对话消息数量超过此值时，自动执行总结
     */
    private Integer count;

    /**
     * 自定义总结提示词（Prompt） 告诉大模型如何总结对话历史，例如：“请用简洁的语言总结以下对话”
     */
    private String prompt;

    /**
     * 总结内容前缀 生成的总结文本前会添加此前缀，用于标识这是总结内容 例如：【对话总结】、【历史摘要】
     */
    private String prefix;

    /**
     * Token 计算器 用于统计对话历史的Token数量，判断是否需要总结
     */
    private TokenCounter counter;

    /**
     * 是否保留原始消息 true：保留原始消息 + 新增总结内容 false：用总结替换掉旧消息，只保留最新总结
     */
    private Boolean keep;

}
