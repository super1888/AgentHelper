package com.spring.ai.core.model.dto;

import java.util.List;
import lombok.Data;

/**
 * ChatOptionsDTO 灵活调用不同模型设置
 * <p>
 * model: 要使用的模型 ID frequencyPenalty: 频率惩罚（-2.0 到 2.0），降低重复令牌的可能性 maxTokens: 生成响应的最大令牌数 presencePenalty: 存在惩罚（-2.0 到 2.0），鼓励谈论新主题 stopSequences:
 * 停止序列列表，遇到时停止生成 temperature: 采样温度（0.0 到 2.0），控制随机性 topK: Top-K 采样参数 topP: Top-P（核采样）参数
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/29
 */
@Data
public class ChatOptionsDTO {


    /**
     * 要使用的模型 ID
     */
    private String model;


    /**
     * 频率惩罚（-2.0 到 2.0），降低重复令牌的可能性
     */
    private Double frequencyPenalty;


    /**
     * 生成响应的最大令牌数
     */
    private Integer maxTokens;


    /**
     * 存在惩罚（-2.0 到 2.0），鼓励谈论新主题
     */
    private Double presencePenalty;


    /**
     * 停止序列列表，遇到时停止生成
     */
    private List<String> stopSequences;


    /**
     * 采样温度（0.0 到 2.0），控制随机性
     */
    private Double temperature;


    /**
     * Top-K 采样参数
     */
    private Integer topK;


    /**
     * Top-P（核采样）参数
     */
    private Double topP;

}
