package com.spring.ai.hooks.domain.dto;

import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetector;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIType;
import com.alibaba.cloud.ai.graph.agent.hook.pii.RedactionStrategy;
import lombok.Builder;
import lombok.Data;

/**
 * 检测和处理对话中的个人身份信息。
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
@Data
@Builder
public class PIIDetectionHookDTO {

    /**
     * 要检测的隐私信息类型 可选：EMAIL、CREDIT_CARD、IP、MAC_ADDRESS、URL、CUSTOM 默认：CUSTOM（自定义）
     */
    private PIIType piiType;

    /**
     * 检测到PII信息后的处理策略 BLOCK：直接拦截请求 REDACT：编辑移除敏感内容 MASK：掩码脱敏（如 138****1234） HASH：哈希加密处理 默认：MASK
     */
    private RedactionStrategy strategy;

    /**
     * 自定义PII检测器（不填则使用框架默认）
     */
    private PIIDetector detector;

    /**
     * 是否对【用户输入消息】进行PII检测与处理 默认：true
     */
    private boolean applyToInput;

    /**
     * 是否对【AI输出结果】进行PII检测与处理 默认：true
     */
    private boolean applyToOutput;

    /**
     * 是否对【工具调用返回结果】进行PII检测与处理 默认：true
     */
    private boolean applyToToolResults;

}
