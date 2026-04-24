package com.spring.ai.core.domain.dto;

import java.util.List;
import lombok.Data;

/**
 * 模型调用参数对象。
 * 对外屏蔽不同大模型供应商在参数命名上的细节差异，统一由工厂层完成转换。
 */
@Data
public class ChatOptionsDTO {

    /**
     * 实际调用的模型标识。
     */
    private String model;

    /**
     * 频率惩罚参数，通常用于降低重复输出。
     */
    private Double frequencyPenalty;

    /**
     * 本次生成允许输出的最大 token 数。
     */
    private Integer maxTokens;

    /**
     * 存在惩罚参数，通常用于鼓励模型引入新话题。
     */
    private Double presencePenalty;

    /**
     * 停止序列列表，命中任一序列后停止生成。
     */
    private List<String> stopSequences;

    /**
     * 温度参数，用于控制输出的随机性。
     */
    private Double temperature;

    /**
     * Top-K 采样参数。
     */
    private Integer topK;

    /**
     * Top-P 采样参数。
     */
    private Double topP;
}
