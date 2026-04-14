package com.spring.ai.interceptors.custom.modelInterceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.spring.ai.user.domain.dto.UserPreferencesDTO;
import org.springframework.ai.chat.messages.SystemMessage;

/**
 * 从长期记忆加载用户偏好
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/7
 */
class PersonalizedPromptInterceptor extends ModelInterceptor {

    private final UserPreferencesDTO store;

    public PersonalizedPromptInterceptor(UserPreferencesDTO store) {
        this.store = store;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 从运行时上下文获取用户ID
        String userId = getUserIdFromContext(request);

        // 从存储加载用户偏好
        UserPreferencesDTO prefs = store.getPreferences(userId);

        // 构建个性化提示
        String personalizedPrompt = buildPersonalizedPrompt(prefs);

        // 更新系统消息（参考 TodoListInterceptor 的实现方式）
        SystemMessage enhancedSystemMessage;
        if (request.getSystemMessage() == null) {
            enhancedSystemMessage = new SystemMessage(personalizedPrompt);
        } else {
            enhancedSystemMessage = new SystemMessage(
                    request.getSystemMessage().getText() + " " + personalizedPrompt
            );
        }

        // 创建增强的请求
        ModelRequest enhancedRequest = ModelRequest.builder(request)
                .systemMessage(enhancedSystemMessage)
                .build();

        // 调用处理器
        return handler.call(enhancedRequest);
    }

    private String getUserIdFromContext(ModelRequest request) {
        // 相当于是从 RunnableConfig 中读取提取用户ID，所以agent调用时要设置 user-id
        return String.valueOf(request.getContext().get("user-id")); // 简化示例
    }

    private String buildPersonalizedPrompt(UserPreferencesDTO prefs) {
        StringBuilder prompt = new StringBuilder("你是一个有用的助手。");

        if (prefs.getCommunicationStyle() != null) {
            prompt.append("沟通风格：").append(prefs.getCommunicationStyle());
        }

        if (prefs.getLanguage() != null) {
            prompt.append("使用语言：").append(prefs.getLanguage());
        }

        if (!prefs.getInterests().isEmpty()) {
            prompt.append("用户兴趣：").append(String.join(", ", prefs.getInterests()));
        }

        return prompt.toString();
    }

    @Override
    public String getName() {
        return "PersonalizedPromptInterceptor";
    }
}
