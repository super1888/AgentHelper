package com.spring.ai.core.config;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.spring.ai.core.node.ConditionEvaluatorNode;
import com.spring.ai.core.node.TextProcessorNode;
import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
/**
 * 集成自定义 Node 到 StateGraph
 * ReplaceStrategy → 直接覆盖旧值（你现在用的）
 * AppendStrategy → 追加到后面
 * MergeStrategy → 合并对象
 * KeepStrategy → 保留旧值，不更新
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/9
 */
public class WorkflowConfiguration {

    public StateGraph customWorkflowGraph(ChatClient.Builder chatClientBuilder) throws Exception{
        // 定义状态管理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("query", new ReplaceStrategy());
            strategies.put("processed_text", new ReplaceStrategy());
            strategies.put("queryVariants", new ReplaceStrategy());
            strategies.put("final_result", new ReplaceStrategy());
            return strategies;
        };

        // 构建 StateGraph
        StateGraph graph = new StateGraph(keyStrategyFactory);

        // 添加自定义 Node
        graph.addNode("processor", node_async(new TextProcessorNode()));
        graph.addNode("condition", node_async(new ConditionEvaluatorNode()));

        // 定义边（流程连接）
        graph.addEdge(StateGraph.START, "processor");
        graph.addEdge("processor", "condition");

        // 条件边：根据 condition node 的结果路由
        graph.addConditionalEdges(
                "condition",
                edge_async(state -> state.value("_condition_result", "short").toString()),
                Map.of("long", "processor",  // 长文本重新处理
                        "short", StateGraph.END  // 短文本结束
                )
        );

        return graph;
    }
}
