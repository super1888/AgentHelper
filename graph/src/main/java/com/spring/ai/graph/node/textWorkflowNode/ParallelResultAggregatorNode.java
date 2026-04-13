package com.spring.ai.graph.node.textWorkflowNode;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/9
 */
public class ParallelResultAggregatorNode implements NodeAction {

    private final String outputKey;

    public ParallelResultAggregatorNode(String outputKey) {
        this.outputKey = outputKey;
    }
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 收集所有并行任务的结果
        List<String> results = new ArrayList<>();

        // 假设并行任务将结果存储在不同的键中
        state.value("result_1").ifPresent(r -> results.add(r.toString()));
        state.value("result_2").ifPresent(r -> results.add(r.toString()));
        state.value("result_3").ifPresent(r -> results.add(r.toString()));

        // 聚合结果
        String aggregatedResult = String.join(" --- ", results);

                Map<String, Object> output = new HashMap<>();
        output.put(outputKey, aggregatedResult);
        return output;
    }
}
