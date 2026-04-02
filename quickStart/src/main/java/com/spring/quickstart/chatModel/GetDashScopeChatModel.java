package com.spring.quickstart.chatModel;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.spring.quickstart.modelApi.GetDashScopeApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/23
 */

@Component
public class GetDashScopeChatModel {

    @Resource
    GetDashScopeApi getDashScopeApi;

    /**
     * 无配置 模型
     *
     * @return
     */
    public DashScopeChatModel getModel() {
        // 创建模型实例
        DashScopeChatModel build = DashScopeChatModel.builder()
                .dashScopeApi(getDashScopeApi.getDashScopeApi(null))
                .build();
        return build;
    }

    /**
     * 无配置 模型
     *
     * @return
     */
    public DashScopeChatModel getChatModel () {
        // 创建模型实例
        DashScopeChatModel build = DashScopeChatModel.builder()
                .dashScopeApi(getDashScopeApi.getDashScopeApi(null))
                .build();
        return build;
    }

    /**
     * 高级配置 模型
     * <p>
     * temperature：控制输出的随机性（0.0-1.0），
     * 值越高越有创造性 maxTokens：限制单次响应的最大 token 数 topP：核采样，控制输出的多样性
     *
     * @return
     */
    public DashScopeChatModel getSeniorModel() {

        DashScopeChatModel build = DashScopeChatModel.builder()
                .dashScopeApi(getDashScopeApi.getDashScopeApi(null))
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-max")
                        .withTemperature(0.7)    // 控制随机性
                        .withMaxToken(2000)      // 最大输出长度
                        .withTopP(0.9)           // 核采样参数
                        .build())
                .build();
        return build;
    }
}
