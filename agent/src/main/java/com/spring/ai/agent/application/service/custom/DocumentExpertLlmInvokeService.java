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
        return call(chatClient, prompt, "未知阶段", "未知模型");
    }

    /**
     * 按阶段和模型上下文调用指定客户端。
     */
    public String call(ChatClient chatClient, String prompt, String stageName, String modelCode) {
        try {
            return CommonTextUtils.defaultText(chatClient.prompt(prompt).call().content(), "");
        } catch (Exception ex) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    String.format("文档专家 Agent 执行失败，阶段：%s，模型：%s，原因：%s",
                            stageName,
                            modelCode,
                            ex.getMessage())
            );
        }
    }

    /**
     * 按模型编码创建客户端并调用。
     */
    public String callByModelCode(String modelCode, String prompt) {
        return callByModelCode(modelCode, prompt, "未知阶段");
    }

    /**
     * 按模型编码并携带阶段上下文调用。
     */
    public String callByModelCode(String modelCode, String prompt, String stageName) {
        return call(documentExpertModelSupportService.createChatClient(modelCode), prompt, stageName, modelCode);
    }

    /**
     * 包装异常上下文，便于在已创建客户端的情况下补充阶段与模型信息。
     */
    public <T> T executeWithStage(String stageName, String modelCode, java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    String.format("文档专家 Agent 执行失败，阶段：%s，模型：%s，原因：%s",
                            stageName,
                            modelCode,
                            ex.getMessage())
            );
        }
    }
}
