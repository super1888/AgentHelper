package com.spring.quickstart.chatModel;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import com.spring.quickstart.modelApi.GetDashScopeApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/29
 */
@Component
public class GetChatModel {


    DashScopeChatOptions options = DashScopeChatOptions.builder()
            .withModel("qwen-max")           // 模型名称
            .withTemperature(0.7)              // Temperature 参数
            .withMaxToken(2000)                // 最大令牌数
            .withTopP(0.9)                     // Top-P 采样
            .build();

    @Resource
    GetDashScopeApi getDashScopeApi;

    public ChatModel creatDashScopeChatModel() {
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(getDashScopeApi.getDashScopeApi(null))
                .defaultOptions(options)
                .build();
        return chatModel;
    }

}
