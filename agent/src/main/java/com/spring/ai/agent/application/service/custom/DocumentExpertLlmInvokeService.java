package com.spring.ai.agent.application.service.custom;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.utils.CommonTextUtils;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 文档专家大模型调用服务。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Component
public class DocumentExpertLlmInvokeService {

    @Resource
    private DocumentExpertModelSupportService documentExpertModelSupportService;

    /**
     * 调用指定客户端。
     */
    public String call(ChatClient chatClient, String prompt) {
        try {
            return CommonTextUtils.defaultText(chatClient.prompt(prompt).call().content(), "");
        } catch (Exception ex) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "调用文档专家 Agent 失败: " + ex.getMessage());
        }
    }

    /**
     * 按模型编码创建客户端并调用。
     */
    public String callByModelCode(String modelCode, String prompt) {
        return call(documentExpertModelSupportService.createChatClient(modelCode), prompt);
    }
}
