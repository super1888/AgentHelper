package com.spring.ai.interceptors.custom.modelInterceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallback;

/**
 * 基于上下文的工具选择
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/7
 */
class ContextualToolInterceptor extends ModelInterceptor {


    private final Map<String, List<ToolCallback>> roleBasedTools;

    public ContextualToolInterceptor(Map<String, List<ToolCallback>> roleBasedTools) {
        this.roleBasedTools = roleBasedTools;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 从上下文获取用户角色
        String userRole = getUserRole(request);

        // 根据角色选择工具
        List<ToolCallback> allowedTools = roleBasedTools.getOrDefault(
                userRole,
                Collections.emptyList()
        );

        // 更新工具选项（注：实际实现需要根据框架API调整）
        // 这里展示概念性代码
        System.out.println("为角色 " + userRole + " 选择了 " + allowedTools.size() + " 个工具");

        return handler.call(request);
    }

    private String getUserRole(ModelRequest request) {
        // 从请求上下文提取用户角色
        return "user"; // 简化示例
    }

    @Override
    public String getName() {
        return "ContextualToolInterceptor";
    }
}
