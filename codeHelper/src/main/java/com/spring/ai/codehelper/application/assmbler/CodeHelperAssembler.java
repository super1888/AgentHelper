package com.spring.ai.codehelper.application.assmbler;

import com.spring.ai.codehelper.domain.dto.CodeHelperMessageDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperSessionDTO;
import com.spring.ai.codehelper.domain.dto.CodeHelperTaskDTO;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionEventRecord;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionRecord;
import com.spring.ai.codehelper.domain.entity.CodeHelperToolLogRecord;
import com.spring.ai.codehelper.domain.response.CodeHelperContextResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperMessageResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperSessionResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperTaskResponse;
import com.spring.ai.codehelper.domain.response.CodeHelperToolLogResponse;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * codeHelper 响应组装器。
 */
public final class CodeHelperAssembler {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CodeHelperAssembler() {
    }

    /**
     * 转换会话响应。
     */
    public static CodeHelperSessionResponse toSessionResponse(CodeHelperSessionDTO session) {
        return CodeHelperSessionResponse.builder()
                .sessionId(session.getSessionId())
                .sessionName(session.getSessionName())
                .workspacePath(session.getWorkspacePath())
                .projectName(session.getProjectName())
                .branchName(session.getBranchName())
                .taskDescription(session.getTaskDescription())
                .modelCode(session.getModelCode())
                .status(session.getStatus())
                .summary(session.getSummary())
                .messages(toMessageResponses(session.getMessages()))
                .tasks(toTaskResponses(session.getTasks()))
                .build();
    }

    /**
     * 转换会话 DTO。
     */
    public static CodeHelperSessionDTO toSessionDTO(CodeHelperSessionRecord record,
                                                    List<CodeHelperMessageDTO> messages,
                                                    List<CodeHelperTaskDTO> tasks,
                                                    List<String> allowedCommands) {
        return CodeHelperSessionDTO.builder()
                .sessionId(record.getSessionCode())
                .sessionName(record.getSessionName())
                .workspacePath(record.getWorkspacePath())
                .projectName(record.getProjectName())
                .branchName(record.getBranchName())
                .taskDescription(record.getTaskDescription())
                .modelCode(record.getModelCode())
                .status(record.getSessionStatus())
                .summary(record.getSummarySnapshot())
                .messages(messages)
                .tasks(tasks)
                .allowedCommands(allowedCommands)
                .build();
    }

    /**
     * 转换会话事件 DTO。
     */
    public static CodeHelperMessageDTO toMessageDTO(CodeHelperSessionEventRecord event, String fallbackTime) {
        return CodeHelperMessageDTO.builder()
                .role(event.getEventRole())
                .content(event.getEventContent())
                .timestamp(event.getCreateTime() == null ? fallbackTime : event.getCreateTime().format(TIME_FORMATTER))
                .build();
    }

    /**
     * 转换工具日志任务 DTO。
     */
    public static CodeHelperTaskDTO toTaskDTO(CodeHelperToolLogRecord log, String detail) {
        return CodeHelperTaskDTO.builder()
                .taskId(String.valueOf(log.getId()))
                .title(log.getToolName())
                .status(log.getSuccessFlag() != null && log.getSuccessFlag() == 1 ? "SUCCESS" : "FAILED")
                .detail(detail)
                .changedFiles(new ArrayList<>())
                .build();
    }

    /**
     * 转换工具日志响应。
     */
    public static CodeHelperToolLogResponse toToolLogResponse(CodeHelperToolLogRecord log) {
        return CodeHelperToolLogResponse.builder()
                .logId(log.getId())
                .sessionId(log.getSessionCode())
                .toolName(log.getToolName())
                .riskLevel(log.getRiskLevel())
                .success(log.getSuccessFlag() != null && log.getSuccessFlag() == 1)
                .requestJson(log.getRequestJson())
                .responseText(log.getResponseText())
                .durationMillis(log.getDurationMillis())
                .errorMessage(log.getErrorMessage())
                .createTime(log.getCreateTime() == null ? null : log.getCreateTime().format(TIME_FORMATTER))
                .build();
    }

    /**
     * 转换上下文响应。
     */
    public static CodeHelperContextResponse toContextResponse(CodeHelperSessionDTO session) {
        return CodeHelperContextResponse.builder()
                .sessionId(session.getSessionId())
                .summary(session.getSummary())
                .recentMessages(toMessageResponses(session.getMessages()))
                .tasks(toTaskResponses(session.getTasks()))
                .build();
    }

    /**
     * 转换消息响应列表。
     */
    public static List<CodeHelperMessageResponse> toMessageResponses(List<CodeHelperMessageDTO> messages) {
        return messages.stream().map(CodeHelperAssembler::toMessageResponse).toList();
    }

    /**
     * 转换单条消息响应。
     */
    public static CodeHelperMessageResponse toMessageResponse(CodeHelperMessageDTO message) {
        return CodeHelperMessageResponse.builder()
                .role(message.getRole())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();
    }

    /**
     * 转换任务响应列表。
     */
    public static List<CodeHelperTaskResponse> toTaskResponses(List<CodeHelperTaskDTO> tasks) {
        return tasks.stream().map(CodeHelperAssembler::toTaskResponse).toList();
    }

    /**
     * 转换单条任务响应。
     */
    public static CodeHelperTaskResponse toTaskResponse(CodeHelperTaskDTO task) {
        return CodeHelperTaskResponse.builder()
                .taskId(task.getTaskId())
                .title(task.getTitle())
                .status(task.getStatus())
                .detail(task.getDetail())
                .changedFiles(task.getChangedFiles())
                .build();
    }
}
