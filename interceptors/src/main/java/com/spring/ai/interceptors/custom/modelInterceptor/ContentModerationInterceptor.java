package com.spring.ai.interceptors.custom.modelInterceptor;


import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

/**
 * 内容审核 Interceptor 过滤敏感词
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/2
 */
public class ContentModerationInterceptor extends ModelInterceptor {

    private final List<String> blockedWords;


    public ContentModerationInterceptor(List<String> blockedWords) {
        this.blockedWords = blockedWords;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 检查输入
        for (Message msg : request.getMessages()) {
            String content = msg.getText().toLowerCase();
            for (String blocked : blockedWords) {
                if (content.contains(blocked)) {
                    return ModelResponse.of(new AssistantMessage("检测到不适当的内容，请修改您的输入"));
                }
            }
        }

        return handler.call(request);
    }

    @Override
    public String getName() {
        return "ContentModerationInterceptor";
    }
}