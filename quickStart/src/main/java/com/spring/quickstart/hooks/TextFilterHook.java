package com.spring.quickstart.hooks;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.spring.ai.common.utils.TextFilterUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * 文本过滤器
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/31
 */
@HookPositions({HookPosition.BEFORE_MODEL})
public class TextFilterHook extends MessagesModelHook {

    @Override
    public String getName() {
        return "Text Filter";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        // 获取用户是否勾选敏感词 并查询 先模拟一下
        String tip = "恶评";
        List<String> userAWords = List.of("退款", "投诉", "差评");

        for (Message previousMessage : previousMessages) {
//            String check = TextFilterUtils.check(previousMessage.getText(), userAWords, tip);
//            if (StringUtils.isNotBlank(check)) {
//                UserMessage userMessage = new UserMessage("用户输入了恶评，请规劝");
//                return new AgentCommand(List.of(userMessage), UpdatePolicy.REPLACE);
//            }
        }

        return super.beforeModel(previousMessages, config);
    }
}
