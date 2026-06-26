package com.spring.ai.codehelper.application.subagent;

import com.spring.ai.codehelper.application.assmbler.CodeHelperAssembler;
import com.spring.ai.codehelper.application.manager.CodeHelperPromptManager;
import com.spring.ai.codehelper.domain.dto.CodeHelperAgentDecisionDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperMessageDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperSessionDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperSubAgentDefinitionDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperTaskDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperToolCallDTO;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionEventRecord;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionRecord;
import com.spring.ai.codehelper.domain.request.CodeHelperSubAgentRunRequest;
import com.spring.ai.codehelper.domain.response.CodeHelperSubAgentRunResponse;
import com.spring.ai.codehelper.service.CodeHelperSessionEventRecordService;
import com.spring.ai.codehelper.service.CodeHelperSessionRecordService;
import com.spring.ai.common.utils.CommonJsonUtils;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.core.application.manager.CoreApplicationManager;
import com.spring.ai.tools.codehelper.CodeHelperToolRequest;
import com.spring.ai.tools.codehelper.CodeHelperToolResult;
import com.spring.ai.tools.codehelper.CodeHelperWorkspaceToolExecutor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * codeHelper 子 Agent 调度器。
 * 负责选择子 Agent、构造角色提示词、执行低风险工具并返回结构化结果。
 *
 * @author zhouqi
 * @since 2026/6/26
 */
@Component
public class CodeHelperSubAgentManager {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int MAX_AUTO_TOOL_CALLS = 3;

    @Resource
    private CodeHelperSubAgentRegistry subAgentRegistry;

    @Resource
    private CodeHelperSessionRecordService sessionRecordService;

    @Resource
    private CodeHelperSessionEventRecordService sessionEventRecordService;

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private CoreApplicationManager coreApplicationManager;

    @Resource
    private CommonJsonUtils commonJsonUtils;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private CodeHelperPromptManager codeHelperPromptManager;

    @Resource
    private CodeHelperWorkspaceToolExecutor workspaceToolExecutor;

    /**
     * 查询内置子 Agent 列表。
     *
     * @return 子 Agent 定义列表
     */
    public List<CodeHelperSubAgentDefinitionDTO> listSubAgents() {
        return subAgentRegistry.listDefinitions();
    }

    /**
     * 运行一个子 Agent 任务。
     *
     * @param sessionId 会话编号
     * @param request 子 Agent 请求
     * @return 子 Agent 执行结果
     */
    public CodeHelperSubAgentRunResponse runSubAgent(String sessionId, CodeHelperSubAgentRunRequest request) {
        validateRequest(request);
        CodeHelperSessionRecord record = requireSessionRecord(sessionId);
        CodeHelperSessionDTO session = loadSession(record);
        CodeHelperSubAgentDefinitionDTO agent = StringUtils.hasText(request.getAgentType())
                ? subAgentRegistry.requireDefinition(request.getAgentType())
                : subAgentRegistry.selectDefinition(request.getTask());
        String modelCode = resolveModelCode(session, request);
        long start = System.currentTimeMillis();

        CodeHelperAgentDecisionDTO decision = decide(session, agent, request, modelCode);
        List<CodeHelperToolCallDTO> safeToolCalls = filterExecutableToolCalls(agent, request, decision.getToolCalls());
        List<String> toolResults = executeSafeToolCalls(session, safeToolCalls);
        String result = buildResult(decision, toolResults);

        return CodeHelperSubAgentRunResponse.builder()
                .taskId(UUID.randomUUID().toString().replace("-", ""))
                .agent(agent)
                .task(request.getTask().trim())
                .result(result)
                .requireConfirmation(decision.isRequireConfirmation() || hasBlockedToolCalls(decision.getToolCalls(), safeToolCalls))
                .toolCalls(decision.getToolCalls())
                .toolResults(toolResults)
                .costMs(System.currentTimeMillis() - start)
                .build();
    }

    private CodeHelperAgentDecisionDTO decide(CodeHelperSessionDTO session,
                                              CodeHelperSubAgentDefinitionDTO agent,
                                              CodeHelperSubAgentRunRequest request,
                                              String modelCode) {
        try {
            ChatModel chatModel = coreApplicationManager.createChatModel(modelCode);
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            String content = chatClient.prompt()
                    .system(buildSubAgentPrompt(session, agent))
                    .user(buildSubAgentUserPrompt(request))
                    .call()
                    .content();
            return parseDecision(content);
        } catch (RuntimeException exception) {
            return CodeHelperAgentDecisionDTO.builder()
                    .assistantReply("子 Agent 调用失败：" + exception.getMessage())
                    .toolCalls(List.of())
                    .requireConfirmation(false)
                    .modelDriven(false)
                    .build();
        }
    }

    private String buildSubAgentPrompt(CodeHelperSessionDTO session, CodeHelperSubAgentDefinitionDTO agent) {
        return String.join("\n\n",
                codeHelperPromptManager.buildSystemPrompt(session),
                "# 当前子 Agent 角色\n" + agent.getSystemPrompt(),
                "# 子 Agent 约束\n"
                        + "你只能处理分配给你的子任务，不要假装已经完成主任务。\n"
                        + "允许工具：" + String.join(", ", agent.getAllowedTools()) + "\n"
                        + "是否允许写入：" + Boolean.TRUE.equals(agent.getWriteAllowed()) + "\n"
                        + "若需要写入、Shell 或 Git 操作，requireConfirmation 必须为 true。"
        );
    }

    private String buildSubAgentUserPrompt(CodeHelperSubAgentRunRequest request) {
        return """
                子任务：
                %s

                请只输出 JSON：
                {
                  "assistantReply": "你的阶段结论",
                  "requireConfirmation": false,
                  "toolCalls": [
                    {"toolName": "grep", "arguments": {"keyword": "Controller"}}
                  ]
                }
                如果不需要工具，toolCalls 返回空数组。
                """.formatted(request.getTask()).trim();
    }

    private CodeHelperAgentDecisionDTO parseDecision(String content) {
        if (!StringUtils.hasText(content)) {
            return modelReply("子 Agent 未返回内容");
        }
        String json = extractJson(content.trim());
        if (!looksLikeJsonObject(json)) {
            return modelReply(content.trim());
        }
        Map<String, Object> decisionMap = commonJsonUtils.parseMap(json);
        return CodeHelperAgentDecisionDTO.builder()
                .assistantReply(stringValue(decisionMap.get("assistantReply"), content.trim()))
                .requireConfirmation(booleanValue(decisionMap.get("requireConfirmation")))
                .toolCalls(parseToolCalls(decisionMap.get("toolCalls")))
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
            toolCalls.add(CodeHelperToolCallDTO.builder()
                    .toolName(toolName.trim())
                    .arguments(commonJsonUtils.objectMap(rawMap.get("arguments")))
                    .build());
        }
        return toolCalls;
    }

    private List<CodeHelperToolCallDTO> filterExecutableToolCalls(CodeHelperSubAgentDefinitionDTO agent,
                                                                  CodeHelperSubAgentRunRequest request,
                                                                  List<CodeHelperToolCallDTO> toolCalls) {
        if (!Boolean.TRUE.equals(request.getAutoToolCall()) || toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }
        List<String> allowedTools = resolveAllowedTools(agent, request);
        List<CodeHelperToolCallDTO> executable = new ArrayList<>();
        for (CodeHelperToolCallDTO toolCall : toolCalls) {
            if (executable.size() >= MAX_AUTO_TOOL_CALLS) {
                break;
            }
            String toolName = toolCall.getToolName();
            if (!allowedTools.contains(toolName) || isHighRiskTool(toolName) || isWriteTool(toolName)) {
                continue;
            }
            executable.add(toolCall);
        }
        return executable;
    }

    private List<String> executeSafeToolCalls(CodeHelperSessionDTO session, List<CodeHelperToolCallDTO> toolCalls) {
        if (toolCalls.isEmpty()) {
            return List.of();
        }
        List<String> results = new ArrayList<>();
        for (CodeHelperToolCallDTO toolCall : toolCalls) {
            CodeHelperToolResult result = workspaceToolExecutor.execute(CodeHelperToolRequest.builder()
                    .toolName(toolCall.getToolName())
                    .workspacePath(session.getWorkspacePath())
                    .arguments(toolCall.getArguments())
                    .allowedCommands(session.getAllowedCommands())
                    .build());
            results.add(result.getToolName() + "：" + result.getMessage() + "\n" + trimOutput(result.getOutput()));
        }
        return results;
    }

    private CodeHelperSessionDTO loadSession(CodeHelperSessionRecord record) {
        List<CodeHelperMessageDTO> messages = sessionEventRecordService.listBySessionCode(record.getSessionCode(), record.getTenantId()).stream()
                .map(event -> CodeHelperAssembler.toMessageDTO(event, fallbackTime(event)))
                .toList();
        return CodeHelperAssembler.toSessionDTO(record, messages, List.<CodeHelperTaskDTO>of(), parseAllowedCommands(record));
    }

    private List<String> parseAllowedCommands(CodeHelperSessionRecord record) {
        if (!StringUtils.hasText(record.getAllowedCommandsJson())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(record.getAllowedCommandsJson(), new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }

    private CodeHelperSessionRecord requireSessionRecord(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("会话编号不能为空");
        }
        CodeHelperSessionRecord record = sessionRecordService.getBySessionCode(sessionId, currentUserContextSupport.getCurrentTenantIdWithAutoInit());
        if (record == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        return record;
    }

    private List<String> resolveAllowedTools(CodeHelperSubAgentDefinitionDTO agent, CodeHelperSubAgentRunRequest request) {
        if (request.getAllowedTools() == null || request.getAllowedTools().isEmpty()) {
            return agent.getAllowedTools();
        }
        return request.getAllowedTools().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(agent.getAllowedTools()::contains)
                .toList();
    }

    private String resolveModelCode(CodeHelperSessionDTO session, CodeHelperSubAgentRunRequest request) {
        if (StringUtils.hasText(request.getModelCode())) {
            return request.getModelCode().trim();
        }
        if (StringUtils.hasText(session.getModelCode())) {
            return session.getModelCode();
        }
        throw new IllegalArgumentException("请先为会话或子 Agent 请求指定模型");
    }

    private String buildResult(CodeHelperAgentDecisionDTO decision, List<String> toolResults) {
        if (toolResults.isEmpty()) {
            return decision.getAssistantReply();
        }
        return decision.getAssistantReply() + "\n\n工具结果：\n" + String.join("\n\n", toolResults);
    }

    private boolean hasBlockedToolCalls(List<CodeHelperToolCallDTO> toolCalls, List<CodeHelperToolCallDTO> executableToolCalls) {
        return toolCalls != null && toolCalls.size() > executableToolCalls.size();
    }

    private boolean isHighRiskTool(String toolName) {
        return "shell".equals(toolName) || "git_status".equals(toolName) || "git_diff".equals(toolName);
    }

    private boolean isWriteTool(String toolName) {
        return "write_file".equals(toolName) || "edit_file".equals(toolName);
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

    private CodeHelperAgentDecisionDTO modelReply(String content) {
        return CodeHelperAgentDecisionDTO.builder()
                .assistantReply(content)
                .toolCalls(List.of())
                .requireConfirmation(false)
                .modelDriven(false)
                .build();
    }

    private String trimOutput(String output) {
        if (!StringUtils.hasText(output)) {
            return "";
        }
        return output.length() > 1200 ? output.substring(0, 1200) + "..." : output;
    }

    private String fallbackTime(CodeHelperSessionEventRecord event) {
        return event.getCreateTime() == null ? "" : event.getCreateTime().format(TIME_FORMATTER);
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

    private void validateRequest(CodeHelperSubAgentRunRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("子 Agent 请求不能为空");
        }
        if (!StringUtils.hasText(request.getTask())) {
            throw new IllegalArgumentException("子 Agent 任务不能为空");
        }
    }
}
