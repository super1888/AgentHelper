package com.spring.ai.core.facotry;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.spring.ai.common.enums.ModelProviderEnum;
import com.spring.ai.core.domain.dto.ChatModelRequest;
import com.spring.ai.core.domain.dto.ChatOptionsDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class GetDashScopeChatModel {

    @Resource
    private DynamicChatModelFactory dynamicChatModelFactory;

    public DashScopeChatModel getModel() {
        return (DashScopeChatModel) dynamicChatModelFactory.createDashScopeChatModel(null);
    }

    public DashScopeChatModel getChatModel() {
        return getModel();
    }

    public DashScopeChatModel getSeniorModel() {
        ChatOptionsDTO options = new ChatOptionsDTO();
        options.setModel("qwen-max");
        options.setTemperature(0.7);
        options.setMaxTokens(2000);
        options.setTopP(0.9);

        ChatModelRequest request = new ChatModelRequest();
        request.setProvider(ModelProviderEnum.DASHSCOPE.name());
        request.setOptions(options);
        return (DashScopeChatModel) dynamicChatModelFactory.create(request);
    }
}
