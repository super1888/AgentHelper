package com.spring.ai.codehelper.application.manager;

import com.spring.ai.codehelper.application.assmbler.CodeHelperAssembler;
import com.spring.ai.codehelper.config.CodeHelperProperties;
import com.spring.ai.codehelper.domain.dto.CodeHelperAgentDecisionDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperMessageDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperSessionDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperTaskDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperToolCallDTO;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionEventRecord;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionRecord;
import com.spring.ai.codehelper.domain.entity.CodeHelperToolLogRecord;
import com.spring.ai.codehelper.domain.request.CodeHelperCompactRequest;
import com.spring.ai.codehelper.domain.request.CodeHelperMessageRequest;
import com.spring.ai.codehelper.domain.request.CodeHelperPermissionCheckRequest;
import com.spring.ai.codehelper.domain.request.CodeHelperSessionCreateRequest;
import com.spring.ai.codehelper.domain.request.CodeHelperToolExecuteRequest;
import com.spring.ai.codehelper.domain.response.CodeHelperContextResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperPermissionDecisionResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperSessionResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperToolExecutionResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperToolLogResponse;
import com.spring.ai.codehelper.service.CodeHelperSessionEventRecordService;
import com.spring.ai.codehelper.service.CodeHelperSessionRecordService;
import com.spring.ai.codehelper.service.CodeHelperToolLogRecordService;
import com.spring.ai.common.utils.CommonJsonUtils;
import com.spring.ai.common.web.CurrentUserContextSupport;
import com.spring.ai.core.application.manager.CoreApplicationManager;
import com.spring.ai.core.domain.response.ModelOptionResponse;
import com.spring.ai.tools.codehelper.CodeHelperToolDescriptor;
import com.spring.ai.tools.codehelper.CodeHelperToolRequest;
import com.spring.ai.tools.codehelper.CodeHelperToolResult;
import com.spring.ai.tools.codehelper.CodeHelperWorkspaceToolExecutor;
import jakarta.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * codeHelper 核心编排器。
 *
 * <p>负责会话、任务、上下文压缩和工具调用流程，底层文件与命令能力由 tools 模块承载。</p>
 */
@Component
public class CodeHelperApplicationManager {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private CodeHelperProperties codeHelperProperties;

    @Resource
    private CodeHelperWorkspaceToolExecutor workspaceToolExecutor;

    @Resource
    private CodeHelperPromptManager codeHelperPromptManager;

    @Resource
    private CodeHelperRuntimeManager codeHelperRuntimeManager;

    @Resource
    private CodeHelperSessionRecordService sessionRecordService;

    @Resource
    private CodeHelperSessionEventRecordService sessionEventRecordService;

    @Resource
    private CodeHelperToolLogRecordService toolLogRecordService;

    @Resource
    private CurrentUserContextSupport currentUserContextSupport;

    @Resource
    private CommonJsonUtils commonJsonUtils;

    @Resource
    private CoreApplicationManager coreApplicationManager;

    /**
     * 创建新的代码助手会话，并持久化工作区、任务目标和允许命令。
     */
    @Transactional(rollbackFor = Exception.class)
    public CodeHelperSessionResponse createSession(CodeHelperSessionCreateRequest request) {
        validateSessionRequest(request);
        Long tenantId = currentTenantId();
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String workspacePath = normalizeWorkspacePath(request.getWorkspacePath());
        List<String> allowedCommands = resolveAllowedCommands(request.getAllowedCommands());

        CodeHelperSessionRecord record = new CodeHelperSessionRecord();
        record.setSessionCode(sessionId);
        record.setSessionName(resolveSessionName(request));
        record.setTenantId(tenantId);
        record.setOwnerUserId(currentUserId());
        record.setOwnerUserName(currentUserName());
        record.setWorkspacePath(workspacePath);
        record.setProjectName(trimToDefault(request.getProjectName(), "未命名项目"));
        record.setBranchName(trimToDefault(request.getBranchName(), "main"));
        record.setTaskDescription(trimToDefault(request.getTaskDescription(), ""));
        record.setModelCode(resolveModelCode(request.getModelCode()));
        record.setSessionStatus("ACTIVE");
        record.setSummarySnapshot("会话已创建");
        record.setAllowedCommandsJson(commonJsonUtils.toJson(allowedCommands));
        sessionRecordService.save(record);

        appendEvent(record, "system", "会话创建成功，工作区已绑定：" + workspacePath);
        return CodeHelperAssembler.toSessionResponse(loadSession(record));
    }

    /**
     * 向代码助手会话发送消息。
     */
    @Transactional(rollbackFor = Exception.class)
    public CodeHelperSessionResponse sendMessage(String sessionId, CodeHelperMessageRequest request) {
        CodeHelperSessionRecord record = requireSessionRecord(sessionId);
        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        appendEvent(record, "user", request.getContent());
        CodeHelperSessionDTO session = loadSession(record);
        CodeHelperAgentDecisionDTO decision = normalizeDecision(decideNextStep(session, request), session);
        String assistantReply = formatAssistantReply(decision);
        if (decision.isRequireConfirmation()) {
            appendEvent(record, "assistant", assistantReply + "\n当前操作需要确认，请先确认高风险步骤。");
        } else {
            appendEvent(record, "assistant", assistantReply);
            List<CodeHelperToolResult> toolResults = executeToolCalls(record, session, decision.getToolCalls());
            if (decision.getToolCalls() != null && !decision.getToolCalls().isEmpty()) {
                appendEvent(record, "assistant", buildToolCompletionReply(toolResults));
            }
        }
        refreshSummary(record);
        return CodeHelperAssembler.toSessionResponse(loadSession(record));
    }

    /**
     * 执行一个显式工具调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public CodeHelperToolExecutionResponse executeTool(CodeHelperToolExecuteRequest request) {
        if (request == null || !StringUtils.hasText(request.getSessionId())) {
            throw new IllegalArgumentException("会话编号不能为空");
        }
        CodeHelperSessionRecord record = requireSessionRecord(request.getSessionId());
        CodeHelperPermissionDecisionResponse decision = checkPermission(toPermissionRequest(request));
        if (!decision.isAllowed()) {
            throw new IllegalArgumentException(decision.getReason());
        }
        CodeHelperToolRequest toolRequest = CodeHelperToolRequest.builder()
                .toolName(request.getToolName())
                .workspacePath(resolveWorkspacePathFromSession(record, request.getWorkspacePath()))
                .arguments(request.getArguments())
                .allowedCommands(resolveAllowedCommands(request.getAllowedCommands()))
                .build();
        CodeHelperToolResult result = workspaceToolExecutor.execute(toolRequest);
        appendToolResult(record, toolRequest, result);
        refreshSummary(record);
        return toToolExecutionResponse(record.getSessionCode(), result);
    }

    /**
     * 权限校验。
     */
    public CodeHelperPermissionDecisionResponse checkPermission(CodeHelperPermissionCheckRequest request) {
        if (request == null || !StringUtils.hasText(request.getToolName())) {
            throw new IllegalArgumentException("工具名称不能为空");
        }
        String toolName = request.getToolName().trim();
        String riskLevel = resolveRiskLevel(toolName, request.getCommand());
        boolean allowed = !"HIGH".equals(riskLevel) || hasAllowedCommand(request.getAllowedCommands(), request.getCommand());
        String reason = allowed ? "允许执行" : "高风险命令未加入允许列表";
        return CodeHelperPermissionDecisionResponse.builder()
                .toolName(toolName)
                .allowed(allowed)
                .reason(reason)
                .riskLevel(riskLevel)
                .build();
    }

    /**
     * 生成当前会话上下文快照。
     */
    public CodeHelperContextResponse buildContext(String sessionId) {
        return CodeHelperAssembler.toContextResponse(loadSession(requireSessionRecord(sessionId)));
    }

    /**
     * 构建当前会话的系统提示词。
     */
    public String buildSystemPrompt(String sessionId) {
        return codeHelperPromptManager.buildSystemPrompt(loadSession(requireSessionRecord(sessionId)));
    }

    /**
     * 查询编程助手工具清单。
     */
    public List<CodeHelperToolDescriptor> listTools() {
        return workspaceToolExecutor.listTools();
    }

    /**
     * 查询编程助手可选择的核心模型配置。
     */
    public List<ModelOptionResponse> listModelOptions() {
        return coreApplicationManager.listEnabledModelOptions();
    }

    /**
     * 压缩会话上下文。
     */
    @Transactional(rollbackFor = Exception.class)
    public CodeHelperContextResponse compactContext(String sessionId, CodeHelperCompactRequest request) {
        CodeHelperSessionRecord record = requireSessionRecord(sessionId);
        String hint = request == null || !StringUtils.hasText(request.getSummaryHint())
                ? "自动压缩上下文"
                : request.getSummaryHint().trim();
        refreshSummary(record);
        record.setSummarySnapshot(hint + " | " + record.getSummarySnapshot());
        sessionRecordService.updateById(record);
        appendEvent(record, "system", "上下文已压缩");
        return CodeHelperAssembler.toContextResponse(loadSession(record));
    }

    /**
     * 列出当前租户的会话。
     */
    public List<CodeHelperSessionResponse> listSessions() {
        Long tenantId = currentTenantId();
        return sessionRecordService.lambdaQuery()
                .eq(CodeHelperSessionRecord::getTenantId, tenantId)
                .list()
                .stream()
                .map(this::loadSession)
                .map(CodeHelperAssembler::toSessionResponse)
                .toList();
    }

    /**
     * 查询工具执行日志。
     */
    public List<CodeHelperToolLogResponse> listToolLogs(String sessionId) {
        Long tenantId = currentTenantId();
        List<CodeHelperToolLogRecord> logs = StringUtils.hasText(sessionId)
                ? toolLogRecordService.listBySessionCode(sessionId, tenantId)
                : toolLogRecordService.listByTenantId(tenantId);
        return logs.stream().map(CodeHelperAssembler::toToolLogResponse).toList();
    }

    private CodeHelperSessionDTO loadSession(CodeHelperSessionRecord record) {
        List<CodeHelperMessageDTO> messages = sessionEventRecordService
                .listBySessionCode(record.getSessionCode(), record.getTenantId())
                .stream()
                .map(event -> CodeHelperAssembler.toMessageDTO(event, now()))
                .toList();
        List<CodeHelperTaskDTO> tasks = toolLogRecordService
                .listBySessionCode(record.getSessionCode(), record.getTenantId())
                .stream()
                .map(this::toTaskDTO)
                .toList();
        return CodeHelperAssembler.toSessionDTO(record, messages, tasks, parseAllowedCommands(record.getAllowedCommandsJson()));
    }

    private CodeHelperTaskDTO toTaskDTO(CodeHelperToolLogRecord log) {
        String detail = trimForSummary(log.getErrorMessage() == null ? log.getResponseText() : log.getErrorMessage());
        return CodeHelperAssembler.toTaskDTO(log, detail);
    }

    private CodeHelperAgentDecisionDTO planNextStep(CodeHelperSessionDTO session, CodeHelperMessageRequest request) {
        String content = request.getContent().trim();
        List<CodeHelperToolCallDTO> toolCalls = new ArrayList<>();
        boolean requireConfirmation = false;
        String assistantReply = "已收到任务，开始分析工作区。";

        if (content.contains("修改") || content.contains("实现") || content.contains("修复")) {
            toolCalls.add(CodeHelperToolCallDTO.builder()
                    .toolName("grep")
                    .workspacePath(session.getWorkspacePath())
                    .arguments(Map.of("keyword", guessKeyword(content)))
                    .allowedCommands(session.getAllowedCommands())
                    .build());
            assistantReply = "我会先定位相关代码，再给出修改建议。";
        }
        if (content.contains("执行") || content.contains("编译") || content.contains("测试") || content.contains("构建")) {
            toolCalls.add(CodeHelperToolCallDTO.builder()
                    .toolName("shell")
                    .workspacePath(session.getWorkspacePath())
                    .arguments(Map.of("command", "mvn test", "timeoutSeconds", 60))
                    .allowedCommands(session.getAllowedCommands())
                    .build());
            assistantReply = "我会先执行验证命令，确认当前状态。";
            requireConfirmation = true;
        }
        if (content.contains("查看") || content.contains("列出") || content.contains("目录")) {
            toolCalls.add(CodeHelperToolCallDTO.builder()
                    .toolName("list_directory")
                    .workspacePath(session.getWorkspacePath())
                    .arguments(Map.of("path", "."))
                    .allowedCommands(session.getAllowedCommands())
                    .build());
            assistantReply = "我会先列出工作区目录，帮助确认项目结构。";
        }
        if (toolCalls.isEmpty()) {
            assistantReply = "我已收到你的指令。当前没有匹配到自动工具动作，你可以继续说明要查看、修改、测试或构建的目标。";
        }
        return CodeHelperAgentDecisionDTO.builder()
                .assistantReply(assistantReply)
                .toolCalls(toolCalls)
                .requireConfirmation(requireConfirmation)
                .modelDriven(false)
                .build();
    }

    private CodeHelperAgentDecisionDTO decideNextStep(CodeHelperSessionDTO session, CodeHelperMessageRequest request) {
        CodeHelperAgentDecisionDTO modelDecision = codeHelperRuntimeManager.decide(session, request);
        if (modelDecision != null && StringUtils.hasText(modelDecision.getAssistantReply())) {
            return modelDecision;
        }
        return planNextStep(session, request);
    }

    private String formatAssistantReply(CodeHelperAgentDecisionDTO decision) {
        String prefix = decision.isModelDriven() ? "[MODEL] " : "[RULE] ";
        return prefix + decision.getAssistantReply();
    }

    private CodeHelperAgentDecisionDTO normalizeDecision(CodeHelperAgentDecisionDTO decision, CodeHelperSessionDTO session) {
        if (decision == null) {
            return CodeHelperAgentDecisionDTO.builder()
                    .assistantReply("已收到任务，当前未生成工具调用计划。")
                    .toolCalls(List.of())
                    .requireConfirmation(false)
                    .modelDriven(false)
                    .build();
        }
        List<CodeHelperToolCallDTO> normalizedCalls = decision.getToolCalls() == null
                ? List.of()
                : decision.getToolCalls().stream()
                .filter(call -> call != null && StringUtils.hasText(call.getToolName()))
                .map(call -> normalizeToolCall(call, session))
                .toList();
        if (!StringUtils.hasText(decision.getAssistantReply())) {
            decision.setAssistantReply(buildFallbackAssistantReply(normalizedCalls));
        }
        boolean requireConfirmation = decision.isRequireConfirmation() || normalizedCalls.stream().anyMatch(this::isHighRiskToolCall);
        decision.setToolCalls(normalizedCalls);
        decision.setRequireConfirmation(requireConfirmation);
        return decision;
    }

    private String buildFallbackAssistantReply(List<CodeHelperToolCallDTO> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return "已收到任务，当前没有生成工具调用。";
        }
        String firstTool = toolCalls.get(0).getToolName();
        return "我会先执行 " + firstTool + "，并根据结果继续处理。";
    }

    private String buildToolCompletionReply(List<CodeHelperToolResult> toolResults) {
        if (toolResults == null || toolResults.isEmpty()) {
            return "本次没有执行工具。你可以继续补充要查看、修改或验证的目标。";
        }
        long successCount = toolResults.stream().filter(CodeHelperToolResult::isSuccess).count();
        String toolNames = toolResults.stream()
                .map(CodeHelperToolResult::getToolName)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("、"));
        if (!StringUtils.hasText(toolNames)) {
            return "本次工具调用已完成。";
        }
        String summary = toolResults.stream()
                .map(this::summarizeToolResult)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
        return "工具执行完成：" + toolNames + "。成功 " + successCount + "/" + toolResults.size() + "。\n"
                + summary
                + "\n你可以继续下达下一步指令，例如让我读取某个文件、解释结果或生成修改方案。";
    }

    private CodeHelperToolCallDTO normalizeToolCall(CodeHelperToolCallDTO toolCall, CodeHelperSessionDTO session) {
        return CodeHelperToolCallDTO.builder()
                .toolName(toolCall.getToolName().trim())
                .workspacePath(StringUtils.hasText(toolCall.getWorkspacePath()) ? toolCall.getWorkspacePath() : session.getWorkspacePath())
                .arguments(toolCall.getArguments() == null ? Map.of() : toolCall.getArguments())
                .allowedCommands(toolCall.getAllowedCommands() == null || toolCall.getAllowedCommands().isEmpty()
                        ? session.getAllowedCommands()
                        : toolCall.getAllowedCommands())
                .build();
    }

    private boolean isHighRiskToolCall(CodeHelperToolCallDTO toolCall) {
        Object command = toolCall.getArguments() == null ? null : toolCall.getArguments().get("command");
        return "HIGH".equals(resolveRiskLevel(toolCall.getToolName(), command == null ? null : String.valueOf(command)));
    }

    private List<CodeHelperToolResult> executeToolCalls(CodeHelperSessionRecord record, CodeHelperSessionDTO session, List<CodeHelperToolCallDTO> toolCalls) {
        List<CodeHelperToolResult> results = new ArrayList<>();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return results;
        }
        for (CodeHelperToolCallDTO toolCall : toolCalls) {
            CodeHelperToolRequest request = CodeHelperToolRequest.builder()
                    .toolName(toolCall.getToolName())
                    .workspacePath(toolCall.getWorkspacePath())
                    .arguments(toolCall.getArguments())
                    .allowedCommands(toolCall.getAllowedCommands())
                    .build();
            CodeHelperToolResult result;
            try {
                result = workspaceToolExecutor.execute(request);
            } catch (RuntimeException exception) {
                result = CodeHelperToolResult.builder()
                        .toolName(toolCall.getToolName())
                        .riskLevel(resolveRiskLevel(toolCall.getToolName(), null))
                        .success(false)
                        .message(exception.getMessage())
                        .output(null)
                        .durationMillis(0L)
                        .build();
            }
            appendToolResult(record, request, result);
            results.add(result);
        }
        return results;
    }

    private String summarizeToolResult(CodeHelperToolResult result) {
        if (result == null) {
            return "";
        }
        String prefix = result.isSuccess() ? "- " + result.getToolName() + "：成功" : "- " + result.getToolName() + "：失败";
        String content = result.isSuccess() ? result.getOutput() : result.getMessage();
        String summary = trimForToolSummary(content);
        return StringUtils.hasText(summary) ? prefix + "，" + summary : prefix;
    }

    private String trimForToolSummary(String value) {
        if (!StringUtils.hasText(value)) {
            return "无详细输出";
        }
        String normalized = value.trim().replace("\r\n", "\n");
        String[] lines = normalized.split("\n");
        String compact = java.util.Arrays.stream(lines)
                .filter(StringUtils::hasText)
                .limit(5)
                .collect(Collectors.joining("；"));
        return compact.length() > 420 ? compact.substring(0, 420) + "..." : compact;
    }

    private void appendToolResult(CodeHelperSessionRecord record, CodeHelperToolRequest request, CodeHelperToolResult result) {
        appendEvent(record, "system", "工具执行：" + result.getToolName() + " | 成功=" + result.isSuccess());
        CodeHelperToolLogRecord log = new CodeHelperToolLogRecord();
        log.setSessionCode(record.getSessionCode());
        log.setTenantId(record.getTenantId());
        log.setToolName(result.getToolName());
        log.setRiskLevel(result.getRiskLevel());
        log.setSuccessFlag(result.isSuccess() ? 1 : 0);
        log.setRequestJson(commonJsonUtils.toJson(request));
        log.setResponseText(result.getOutput());
        log.setDurationMillis(result.getDurationMillis());
        log.setErrorMessage(result.isSuccess() ? null : result.getMessage());
        toolLogRecordService.save(log);
    }

    private void appendEvent(CodeHelperSessionRecord record, String role, String content) {
        CodeHelperSessionEventRecord event = new CodeHelperSessionEventRecord();
        event.setSessionCode(record.getSessionCode());
        event.setTenantId(record.getTenantId());
        event.setEventSequence(sessionEventRecordService.nextSequence(record.getSessionCode(), record.getTenantId()));
        event.setEventRole(role);
        event.setEventContent(content);
        sessionEventRecordService.save(event);
    }

    private void refreshSummary(CodeHelperSessionRecord record) {
        List<CodeHelperSessionEventRecord> events = sessionEventRecordService.listBySessionCode(record.getSessionCode(), record.getTenantId());
        String recent = events.stream()
                .skip(Math.max(0, events.size() - 5L))
                .map(event -> event.getEventRole() + ":" + trimForSummary(event.getEventContent()))
                .collect(Collectors.joining(" | "));
        record.setSummarySnapshot("workspace=" + record.getWorkspacePath()
                + ", task=" + trimForSummary(record.getTaskDescription())
                + ", recent=" + recent);
        sessionRecordService.updateById(record);
    }

    private void validateSessionRequest(CodeHelperSessionCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("会话创建参数不能为空");
        }
    }

    private CodeHelperSessionRecord requireSessionRecord(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("会话编号不能为空");
        }
        CodeHelperSessionRecord record = sessionRecordService.getBySessionCode(sessionId, currentTenantId());
        if (record == null) {
            throw new IllegalArgumentException("会话不存在");
        }
        return record;
    }

    private List<String> resolveAllowedCommands(List<String> allowedCommands) {
        List<String> commands = allowedCommands == null || allowedCommands.isEmpty()
                ? codeHelperProperties.getDefaultAllowedCommands()
                : allowedCommands;
        return commands.stream().filter(StringUtils::hasText).map(String::trim).toList();
    }

    private List<String> parseAllowedCommands(String json) {
        if (!StringUtils.hasText(json)) {
            return codeHelperProperties.getDefaultAllowedCommands();
        }
        Object commands = commonJsonUtils.parseObject(json, List.class);
        if (!(commands instanceof List<?> list)) {
            return codeHelperProperties.getDefaultAllowedCommands();
        }
        return list.stream().map(String::valueOf).toList();
    }

    private String resolveSessionName(CodeHelperSessionCreateRequest request) {
        return StringUtils.hasText(request.getSessionName()) ? request.getSessionName().trim() : "codeHelper-session";
    }

    private String resolveModelCode(String requestModelCode) {
        if (StringUtils.hasText(requestModelCode)) {
            return requestModelCode.trim();
        }
        if (StringUtils.hasText(codeHelperProperties.getDefaultModelCode())) {
            return codeHelperProperties.getDefaultModelCode().trim();
        }
        List<ModelOptionResponse> options = coreApplicationManager.listEnabledModelOptions();
        return options.stream()
                .filter(option -> Boolean.TRUE.equals(option.getDefaultModel()))
                .findFirst()
                .or(() -> options.stream().findFirst())
                .map(ModelOptionResponse::getModelCode)
                .orElse(null);
    }

    private String normalizeWorkspacePath(String workspacePath) {
        String resolvedWorkspacePath = StringUtils.hasText(workspacePath)
                ? workspacePath.trim()
                : codeHelperProperties.getWorkspaceRoot();
        Path workspace = Path.of(resolvedWorkspacePath).toAbsolutePath().normalize();
        if (!Files.exists(workspace) || !Files.isDirectory(workspace)) {
            throw new IllegalArgumentException("工作区路径不存在或不是目录");
        }
        return workspace.toString();
    }

    private String resolveWorkspacePathFromSession(CodeHelperSessionRecord record, String workspacePath) {
        return StringUtils.hasText(workspacePath) ? normalizeWorkspacePath(workspacePath) : record.getWorkspacePath();
    }

    private String trimToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String trimForSummary(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String text = value.trim();
        return text.length() > 180 ? text.substring(0, 180) + "..." : text;
    }

    private String guessKeyword(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        if (content.contains("Controller")) {
            return "Controller";
        }
        if (content.contains("Service")) {
            return "Service";
        }
        if (content.contains("测试")) {
            return "Test";
        }
        String[] words = content.trim().split("\\s+");
        return words.length == 0 ? content.trim() : words[0];
    }

    private String resolveRiskLevel(String toolName, String command) {
        if ("shell".equals(toolName) || "git_status".equals(toolName) || "git_diff".equals(toolName)) {
            return StringUtils.hasText(command) ? "HIGH" : "MEDIUM";
        }
        if ("write_file".equals(toolName) || "edit_file".equals(toolName) || "compact_context".equals(toolName)
                || "todo_update".equals(toolName)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private boolean hasAllowedCommand(List<String> allowedCommands, String command) {
        if (!StringUtils.hasText(command)) {
            return false;
        }
        List<String> commands = allowedCommands == null || allowedCommands.isEmpty()
                ? codeHelperProperties.getDefaultAllowedCommands()
                : allowedCommands;
        String normalized = command.trim().toLowerCase();
        return commands.stream()
                .filter(StringUtils::hasText)
                .map(item -> item.trim().toLowerCase())
                .anyMatch(normalized::startsWith);
    }

    private CodeHelperPermissionCheckRequest toPermissionRequest(CodeHelperToolExecuteRequest request) {
        CodeHelperPermissionCheckRequest permissionRequest = new CodeHelperPermissionCheckRequest();
        permissionRequest.setToolName(request.getToolName());
        permissionRequest.setWorkspacePath(request.getWorkspacePath());
        permissionRequest.setCommand(request.getArguments() == null ? null : String.valueOf(request.getArguments().get("command")));
        permissionRequest.setAllowedCommands(request.getAllowedCommands());
        return permissionRequest;
    }

    private CodeHelperToolExecutionResponse toToolExecutionResponse(String sessionCode, CodeHelperToolResult result) {
        return CodeHelperToolExecutionResponse.builder()
                .sessionId(sessionCode)
                .toolName(result.getToolName())
                .success(result.isSuccess())
                .riskLevel(result.getRiskLevel())
                .message(result.getMessage())
                .output(result.getOutput())
                .durationMillis(result.getDurationMillis())
                .build();
    }

    private Long currentTenantId() {
        return currentUserContextSupport.getCurrentTenantIdWithAutoInit();
    }

    private Long currentUserId() {
        try {
            return currentUserContextSupport.getCurrentUserId();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String currentUserName() {
        return currentUserContextSupport.getCurrentUserName();
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
}
