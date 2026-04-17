package com.spring.ai.hooks.factory.impl;

import static com.spring.ai.common.utils.BaseUtils.getOrDefault;

import com.alibaba.cloud.ai.graph.agent.hook.TokenCounter;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.spring.ai.common.enums.HookTypeEnum;
import com.spring.ai.hooks.domain.dto.SummarizationHookDTO;
import com.spring.ai.hooks.factory.AbstractHookCreator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 摘要 Hook 创建器。
 */
@Component
public class SummarizationHookCreator extends AbstractHookCreator {

    @Override
    public HookTypeEnum getHookType() {
        return HookTypeEnum.SUMMARIZATION;
    }

    @Override
    public Object create(Object dto) {
        SummarizationHookDTO summarizationHookDTO = (SummarizationHookDTO) dto;
        if (summarizationHookDTO == null) {
            throw new IllegalArgumentException("摘要钩子配置不能为空");
        }

        ChatModel model = summarizationHookDTO.getChatModel();
        Integer maxTokens = getOrDefault(summarizationHookDTO.getMaxTokens(), 4000);
        Integer msgToKeep = getOrDefault(summarizationHookDTO.getCount(), 20);
        String prompt = getOrDefault(summarizationHookDTO.getPrompt(), "请对以下对话历史进行简洁总结，保留关键信息");
        String prefix = getOrDefault(summarizationHookDTO.getPrefix(), "【对话总结】");
        TokenCounter counter = summarizationHookDTO.getCounter();
        Boolean keepFirst = getOrDefault(summarizationHookDTO.getKeep(), Boolean.TRUE);

        return SummarizationHook.builder()
                .model(model)
                .maxTokensBeforeSummary(maxTokens)
                .messagesToKeep(msgToKeep)
                .summaryPrompt(prompt)
                .summaryPrefix(prefix)
                .tokenCounter(counter)
                .keepFirstUserMessage(keepFirst)
                .build();
    }
}
