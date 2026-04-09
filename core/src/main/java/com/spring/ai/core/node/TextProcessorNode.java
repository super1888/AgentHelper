package com.spring.ai.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单的文本处理 Node
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/9
 */
public class TextProcessorNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 1. 从状态中获取输入
        String input = state.value("query", "").toString();

        // 2. 执行业务逻辑
        String processedText = input.toUpperCase().trim();

        // 3. 返回更新后的状态
        Map<String, Object> result = new HashMap<>();
        result.put("processed_text", processedText);
        return result;
    }
}