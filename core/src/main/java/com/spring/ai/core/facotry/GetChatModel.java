package com.spring.ai.core.facotry;

import com.spring.ai.common.enums.ModelProviderEnum;
import com.spring.ai.core.domain.dto.ChatModelRequest;
import com.spring.ai.core.domain.dto.ChatOptionsDTO;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class GetChatModel {

    @Resource
    private DynamicChatModelFactory dynamicChatModelFactory;

    public ChatModel creatDashScopeChatModel() {
        ChatOptionsDTO options = new ChatOptionsDTO();
        options.setModel("qwen-max");
        options.setTemperature(0.7);
        options.setMaxTokens(2000);
        options.setTopP(0.9);

        ChatModelRequest request = new ChatModelRequest();
        request.setProvider(ModelProviderEnum.DASHSCOPE.name());
        request.setOptions(options);
        return dynamicChatModelFactory.create(request);
    }
}
