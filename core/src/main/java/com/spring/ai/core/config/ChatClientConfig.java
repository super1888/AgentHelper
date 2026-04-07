package com.spring.ai.core.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/7
 */
@Configuration
public class ChatClientConfig {

//    @Bean
//    public ChatClient openAiChatClient(DashScopeChatModel chatModel) {
//        return ChatClient.create(chatModel);
//    }
//
//    @Bean
//    public ChatClient openAiChatClient(DeepSeekChatModel chatModel) {
//        return ChatClient.create(chatModel);
//    }
//
//    @Bean
//    public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
//        return ChatClient.create(chatModel);
//    }
//
//    @Bean
//    public ChatClient anthropicChatClient(AnthropicChatModel chatModel) {
//        return ChatClient.create(chatModel);
//    }

}
