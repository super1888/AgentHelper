package com.spring.ai.interceptors.custom.modelInterceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

/**
 * 性能监控 Interceptor 模型调用性能监控
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/2
 */
public class ModelPerformanceInterceptor extends ModelInterceptor {

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 请求前记录
        System.out.println("发送请求到模型: " + request.getMessages().size() + " 条消息");

        long startTime = System.currentTimeMillis();

        // 执行实际调用
        ModelResponse response = handler.call(request);

        // 响应后记录
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("模型响应耗时: " + duration + "ms");

        return response;
    }

    @Override
    public String getName() {
        return "ModelPerformanceInterceptor";
    }
}
