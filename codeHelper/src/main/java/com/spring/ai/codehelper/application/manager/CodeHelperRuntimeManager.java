package com.spring.ai.codehelper.application.manager;

import com.spring.ai.codehelper.domain.dto.CodeHelperAgentDecisionDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperSessionDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperToolCallDTO;
import com.spring.ai.codehelper.domain.request.CodeHelperMessageRequest;
import com.spring.ai.common.utils.CommonJsonUtils;
import com.spring.ai.core.application.manager.CoreApplicationManager;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * codeHelper 运行时管理器。
 *
 * <p>负责把会话上下文、分层系统提示词和用户输入发送给 Spring AI ChatModel，并解析模型返回的 JSON 决策。</p>
 */
@Component
public class CodeHelperRuntimeManager {

    @Resource
    private CoreApplicationManager coreApplicationManager;

    @Resource
    private CodeHelperPromptManager codeHelperPromptManager;

    @Resource
    private CommonJsonUtils commonJsonUtils;

    /**
     * 使用模型生成结构化决策；未配置模型时返回 null，由上层使用规则规划兜底。
     */
    public CodeHelperAgentDecisionDTO decide(CodeHelperSessionDTO session, CodeHelperMessageRequest request) {
        String modelCode = resolveModelCode(session, request);
        if (!StringUtils.hasText(modelCode)) {
            return null;
        }
        try {
            ChatModel chatModel = coreApplicationManager.createChatModel(modelCode);
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            String content = chatClient.prompt()
                    .system(codeHelperPromptManager.buildSystemPrompt(session))
                    .user(buildUserPrompt(request))
                    .call()
                    .content();
            return parseDecision(content);
        } catch (RuntimeException exception) {
            return modelReply("模型调用失败，已降级为普通回复：" + exception.getMessage());
        }
    }

    private CodeHelperAgentDecisionDTO parseDecision(String content) {
        if (!StringUtils.hasText(content)) {
            return modelReply("模型未返回内容");
        }
        String text = content.trim();
        String json = extractJson(text);
        if (!looksLikeJsonObject(json)) {
            return modelReply(text);
        }
        Map<String, Object> decisionMap = commonJsonUtils.parseMap(json);
        String assistantReply = stringValue(decisionMap.get("assistantReply"), content.trim());
        boolean requireConfirmation = booleanValue(decisionMap.get("requireConfirmation"));
        List<CodeHelperToolCallDTO> toolCalls = parseToolCalls(decisionMap.get("toolCalls"));
        return CodeHelperAgentDecisionDTO.builder()
                .assistantReply(assistantReply)
                .requireConfirmation(requireConfirmation)
                .toolCalls(toolCalls)
                .modelDriven(true)
                .build();
    }

    private List<CodeHelperToolCallDTO> parseToolCalls(Object value) {
        if (!(value instanceof List<?> rawCalls)) {
            return List.of();
        }
        List<CodeHelperToolCallDTO> toolCalls = new ArrayList<>();
        for (Object rawCall : rawCalls) {
            if (!(rawCall instanceof Map<?, ?> rawMap)) {
                continue;
            }
            String toolName = stringValue(rawMap.get("toolName"), null);
            if (!StringUtils.hasText(toolName)) {
                continue;
            }
            Object arguments = rawMap.get("arguments");
            toolCalls.add(CodeHelperToolCallDTO.builder()
                    .toolName(toolName.trim())
                    .arguments(commonJsonUtils.objectMap(arguments))
                    .build());
        }
        return toolCalls;
    }

    private String extractJson(String content) {
        String normalized = content;
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```[a-zA-Z]*", "").replaceFirst("```$", "").trim();
        }
        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return normalized.substring(start, end + 1);
        }
        return normalized;
    }

    private boolean looksLikeJsonObject(String content) {
        return StringUtils.hasText(content) && content.trim().startsWith("{") && content.trim().endsWith("}");
    }

    private String resolveModelCode(CodeHelperSessionDTO session, CodeHelperMessageRequest request) {
        if (request != null && StringUtils.hasText(request.getModelCode())) {
            return request.getModelCode().trim();
        }
        if (session != null && StringUtils.hasText(session.getModelCode())) {
            return session.getModelCode().trim();
        }
        return null;
    }

    private String buildUserPrompt(CodeHelperMessageRequest request) {
        return """
                用户新输入：
                %s

                请根据系统提示词输出 JSON 决策。
                """.formatted(request.getContent()).trim();
    }

    private CodeHelperAgentDecisionDTO modelReply(String content) {
        return CodeHelperAgentDecisionDTO.builder()
                .assistantReply(content)
                .requireConfirmation(false)
                .toolCalls(List.of())
                .modelDriven(true)
                .build();
    }

    private String stringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }
}
