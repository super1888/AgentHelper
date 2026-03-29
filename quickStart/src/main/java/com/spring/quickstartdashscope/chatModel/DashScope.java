package com.spring.quickstartdashscope.chatModel;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/23
 */

@Component
public class DashScope {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    private DashScopeApi getDashScopeApi() {
        // 创建模型实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();
        return dashScopeApi;
    }

    /**
     * 无配置 模型
     *
     * @return
     */
    public DashScopeChatModel getModel() {
        // 创建模型实例
        DashScopeChatModel build = DashScopeChatModel.builder()
                .dashScopeApi(getDashScopeApi())
                .build();
        return build;
    }

    /**
     * 高级配置 模型
     *
     * temperature：控制输出的随机性（0.0-1.0），值越高越有创造性
     * maxTokens：限制单次响应的最大 token 数
     * topP：核采样，控制输出的多样性
     *
     * @return
     */
    public DashScopeChatModel getSeniorModel() {

        DashScopeChatModel build = DashScopeChatModel.builder()
                .dashScopeApi(getDashScopeApi())
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        .withTemperature(0.7)    // 控制随机性
                        .withMaxToken(2000)      // 最大输出长度
                        .withTopP(0.9)           // 核采样参数
                        .build())
                .build();
        return build;
    }
}
