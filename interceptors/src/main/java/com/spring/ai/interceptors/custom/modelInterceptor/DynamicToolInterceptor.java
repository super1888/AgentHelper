package com.spring.ai.interceptors.custom.modelInterceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * ModelInterceptor 支持在模型调用前动态管理工具：
 * <p>
 * dynamicToolCallbacks：动态添加工具回调，可以在运行时根据上下文添加新的工具 tools：动态筛选工具，指定本次调用可用的工具名称列表。如果为空，则使用所有默认工具
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
public class DynamicToolInterceptor extends ModelInterceptor {

    // 示例：根据上下文动态创建的工具
    private ToolCallback createContextualTool(String context) {
        return FunctionToolCallback.builder("contextual_tool", (String input) -> {
                    return "处理上下文: " + context + ", 输入: " + input;
                })
                .description("根据上下文动态创建的工具")
                .build();
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 从上下文中获取信息，决定添加哪些工具
        Map<String, Object> context = request.getContext();
        String userRole = (String) context.getOrDefault("user_role", "default");

        // 构建修改后的请求
        ModelRequest.Builder builder = ModelRequest.builder(request);

        // 示例 1: 动态添加工具回调
        List<ToolCallback> dynamicTools = new ArrayList<>();
        if ("premium" .equals(userRole)) {
            // 为高级用户添加额外工具
            dynamicTools.add(createContextualTool("premium_feature"));
        }
        builder.dynamicToolCallbacks(dynamicTools);

        // 示例 2: 动态筛选工具（只允许使用指定的工具）
        if (shouldRestrictTools(context)) {
            // 只允许使用 search 和 calculator 工具
            builder.tools(List.of("search", "calculator"));
        }
        // 如果 tools 为空列表，则使用所有默认工具

        ModelRequest modifiedRequest = builder.build();
        return handler.call(modifiedRequest);
    }

    private boolean shouldRestrictTools(Map<String, Object> context) {
        // 根据上下文决定是否限制工具
        return context.containsKey("restrict_tools");
    }

    @Override
    public String getName() {
        return "DynamicToolInterceptor";
    }
}
