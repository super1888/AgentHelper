package com.spring.ai.tools.codehelper;

import com.spring.ai.common.exception.BusinessExceptions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 代码助手工作区工具执行器。
 * 核心职责：为 codeHelper 模块提供受工作区边界保护的文件、搜索、Git 和命令执行能力。
 */
@Component
public class CodeHelperWorkspaceToolExecutor {

    private static final int MAX_READ_CHARS = 20000;
    private static final int MAX_SEARCH_RESULTS = 100;
    private static final long DEFAULT_COMMAND_TIMEOUT_SECONDS = 30L;
    private static final List<String> HIGH_RISK_TOOLS = List.of("shell", "git_status", "git_diff");
    private static final List<String> MEDIUM_RISK_TOOLS = List.of("write_file", "edit_file", "todo_update", "compact_context");

    /**
     * 返回编程助手可用的内置工具清单。
     */
    public List<CodeHelperToolDescriptor> listTools() {
        return List.of(
                descriptor("read_file", "读取文件", "读取工作区内文本文件内容", "LOW", List.of("path")),
                descriptor("write_file", "写入文件", "新建或完整覆盖工作区内文本文件", "MEDIUM", List.of("path", "content")),
                descriptor("edit_file", "精确编辑文件", "通过 oldText/newText 精确替换文件片段", "MEDIUM", List.of("path", "oldText", "newText")),
                descriptor("list_directory", "列目录", "列出工作区内目录的直接子项", "LOW", List.of("path")),
                descriptor("glob", "文件匹配", "按 glob 模式搜索工作区文件", "LOW", List.of("pattern")),
                descriptor("grep", "全文搜索", "在工作区文本文件中搜索关键字", "LOW", List.of("keyword")),
                descriptor("shell", "命令执行", "在工作区执行白名单命令", "HIGH", List.of("command", "timeoutSeconds")),
                descriptor("git_status", "Git 状态", "查看工作区 git status", "HIGH", List.of()),
                descriptor("git_diff", "Git 差异", "查看工作区 git diff", "HIGH", List.of("path")),
                descriptor("todo_update", "任务清单", "记录任务拆分和完成状态", "MEDIUM", List.of("todoText")),
                descriptor("compact_context", "上下文压缩", "根据摘要字段生成结构化上下文快照", "MEDIUM", List.of("summary"))
        );
    }

    /**
     * 根据工具名称执行具体工具。
     */
    public CodeHelperToolResult execute(CodeHelperToolRequest request) {
        validateRequest(request);
        Instant start = Instant.now();
        String toolName = request.getToolName().trim();
        try {
            String output = switch (toolName) {
                case "read_file" -> readFile(request);
                case "write_file" -> writeFile(request);
                case "edit_file" -> editFile(request);
                case "list_directory" -> listDirectory(request);
                case "glob" -> glob(request);
                case "grep" -> grep(request);
                case "shell" -> shell(request);
                case "git_status" -> gitStatus(request);
                case "git_diff" -> gitDiff(request);
                case "todo_update" -> todoUpdate(request);
                case "compact_context" -> compactContext(request);
                default -> throw BusinessExceptions.badRequest("不支持的代码助手工具：" + toolName);
            };
            return buildResult(toolName, true, "工具执行成功", output, start);
        } catch (RuntimeException exception) {
            return buildResult(toolName, false, exception.getMessage(), null, start);
        }
    }

    private String readFile(CodeHelperToolRequest request) {
        Path path = resolveWorkspacePath(request, stringArg(request, "path", true));
        if (!Files.isRegularFile(path)) {
            throw BusinessExceptions.badRequest("待读取路径不是文件");
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (content.length() <= MAX_READ_CHARS) {
                return content;
            }
            return content.substring(0, MAX_READ_CHARS) + "\n... 文件内容已截断 ...";
        } catch (IOException exception) {
            throw BusinessExceptions.badRequest("文件读取失败：" + exception.getMessage());
        }
    }

    private String writeFile(CodeHelperToolRequest request) {
        Path path = resolveWorkspacePath(request, stringArg(request, "path", true));
        String content = stringArg(request, "content", true);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return "文件写入成功：" + workspaceRelative(request, path);
        } catch (IOException exception) {
            throw BusinessExceptions.badRequest("文件写入失败：" + exception.getMessage());
        }
    }

    private String editFile(CodeHelperToolRequest request) {
        Path path = resolveWorkspacePath(request, stringArg(request, "path", true));
        String oldText = stringArg(request, "oldText", true);
        String newText = stringArg(request, "newText", false);
        if (!Files.isRegularFile(path)) {
            throw BusinessExceptions.badRequest("待编辑路径不是文件");
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (!content.contains(oldText)) {
                throw BusinessExceptions.badRequest("文件中未找到待替换片段");
            }
            String updated = content.replace(oldText, newText == null ? "" : newText);
            Files.writeString(path, updated, StandardCharsets.UTF_8);
            return "文件编辑成功：" + workspaceRelative(request, path);
        } catch (IOException exception) {
            throw BusinessExceptions.badRequest("文件编辑失败：" + exception.getMessage());
        }
    }

    private String listDirectory(CodeHelperToolRequest request) {
        Path path = resolveWorkspacePath(request, stringArg(request, "path", false));
        if (!Files.isDirectory(path)) {
            throw BusinessExceptions.badRequest("路径不是目录");
        }
        try (Stream<Path> stream = Files.list(path)) {
            return stream.sorted(Comparator.comparing(Path::toString))
                    .limit(MAX_SEARCH_RESULTS)
                    .map(item -> (Files.isDirectory(item) ? "[DIR] " : "[FILE] ") + workspaceRelative(request, item))
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("目录为空");
        } catch (IOException exception) {
            throw BusinessExceptions.badRequest("目录读取失败：" + exception.getMessage());
        }
    }

    private String glob(CodeHelperToolRequest request) {
        Path workspace = workspaceRoot(request);
        String pattern = stringArg(request, "pattern", true).replace('\\', '/');
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        List<String> results = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(workspace)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> matcher.matches(workspace.relativize(path)))
                    .limit(MAX_SEARCH_RESULTS)
                    .forEach(path -> results.add(workspaceRelative(request, path)));
        } catch (IOException exception) {
            throw BusinessExceptions.badRequest("文件匹配失败：" + exception.getMessage());
        }
        return results.isEmpty() ? "未匹配到文件" : String.join("\n", results);
    }

    private String grep(CodeHelperToolRequest request) {
        Path workspace = workspaceRoot(request);
        String keyword = stringArg(request, "keyword", true);
        List<String> results = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(workspace)) {
            List<Path> files = stream.filter(Files::isRegularFile).limit(3000).toList();
            for (Path file : files) {
                if (results.size() >= MAX_SEARCH_RESULTS || isLikelyBinary(file)) {
                    continue;
                }
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int index = 0; index < lines.size(); index++) {
                    if (lines.get(index).contains(keyword)) {
                        results.add(workspaceRelative(request, file) + ":" + (index + 1) + ": " + lines.get(index).trim());
                    }
                    if (results.size() >= MAX_SEARCH_RESULTS) {
                        break;
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            throw BusinessExceptions.badRequest("全文搜索失败：" + exception.getMessage());
        }
        return results.isEmpty() ? "未搜索到关键字" : String.join("\n", results);
    }

    private String shell(CodeHelperToolRequest request) {
        String command = stringArg(request, "command", true);
        validateCommand(command, request.getAllowedCommands());
        long timeoutSeconds = longArg(request, "timeoutSeconds", DEFAULT_COMMAND_TIMEOUT_SECONDS);
        return executeCommand(request, command, timeoutSeconds);
    }

    private String gitStatus(CodeHelperToolRequest request) {
        return executeCommand(request, "git status --short", DEFAULT_COMMAND_TIMEOUT_SECONDS);
    }

    private String gitDiff(CodeHelperToolRequest request) {
        String path = stringArg(request, "path", false);
        if (StringUtils.hasText(path)) {
            resolveWorkspacePath(request, path);
            return executeCommand(request, "git diff -- " + path, DEFAULT_COMMAND_TIMEOUT_SECONDS);
        }
        return executeCommand(request, "git diff", DEFAULT_COMMAND_TIMEOUT_SECONDS);
    }

    private String todoUpdate(CodeHelperToolRequest request) {
        String todoText = stringArg(request, "todoText", true);
        return "任务清单已更新：\n" + todoText.trim();
    }

    private String compactContext(CodeHelperToolRequest request) {
        String summary = stringArg(request, "summary", true);
        return "上下文压缩摘要：\n" + summary.trim();
    }

    private String executeCommand(CodeHelperToolRequest request, String command, long timeoutSeconds) {
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        processBuilder.directory(workspaceRoot(request).toFile());
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                process.destroyForcibly();
                throw BusinessExceptions.badRequest("命令执行超时");
            }
            return "exitCode=" + process.exitValue() + "\n" + output.trim();
        } catch (IOException exception) {
            throw BusinessExceptions.badRequest("命令执行失败：" + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw BusinessExceptions.badRequest("命令执行被中断");
        }
    }

    private void validateRequest(CodeHelperToolRequest request) {
        if (request == null || !StringUtils.hasText(request.getToolName())) {
            throw BusinessExceptions.badRequest("工具名称不能为空");
        }
        workspaceRoot(request);
    }

    private void validateCommand(String command, List<String> allowedCommands) {
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        List<String> deniedKeywords = List.of("format ", "del ", "rmdir ", "rd ", "shutdown", "reg ", "diskpart", "git push", "git commit");
        if (deniedKeywords.stream().anyMatch(normalized::contains)) {
            throw BusinessExceptions.badRequest("命令包含高危关键字，已拒绝执行");
        }
        if (allowedCommands == null || allowedCommands.isEmpty()) {
            return;
        }
        boolean allowed = allowedCommands.stream()
                .filter(StringUtils::hasText)
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .anyMatch(normalized::startsWith);
        if (!allowed) {
            throw BusinessExceptions.badRequest("命令不在允许列表中");
        }
    }

    private Path workspaceRoot(CodeHelperToolRequest request) {
        if (!StringUtils.hasText(request.getWorkspacePath())) {
            throw BusinessExceptions.badRequest("工作区路径不能为空");
        }
        Path workspace = Path.of(request.getWorkspacePath()).toAbsolutePath().normalize();
        if (!Files.exists(workspace) || !Files.isDirectory(workspace)) {
            throw BusinessExceptions.badRequest("工作区路径不存在或不是目录");
        }
        return workspace;
    }

    private Path resolveWorkspacePath(CodeHelperToolRequest request, String pathValue) {
        Path workspace = workspaceRoot(request);
        if (!StringUtils.hasText(pathValue)) {
            return workspace;
        }
        Path path = workspace.resolve(pathValue).toAbsolutePath().normalize();
        if (!path.startsWith(workspace)) {
            throw BusinessExceptions.badRequest("路径越出工作区边界");
        }
        return path;
    }

    private String workspaceRelative(CodeHelperToolRequest request, Path path) {
        return workspaceRoot(request).relativize(path.toAbsolutePath().normalize()).toString();
    }

    private String stringArg(CodeHelperToolRequest request, String name, boolean required) {
        Object value = args(request).get(name);
        if (value == null) {
            if (required) {
                throw BusinessExceptions.badRequest("参数不能为空：" + name);
            }
            return null;
        }
        String text = String.valueOf(value);
        if (required && !StringUtils.hasText(text)) {
            throw BusinessExceptions.badRequest("参数不能为空：" + name);
        }
        return text;
    }

    private long longArg(CodeHelperToolRequest request, String name, long defaultValue) {
        Object value = args(request).get(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private Map<String, Object> args(CodeHelperToolRequest request) {
        if (request.getArguments() == null) {
            return new HashMap<>();
        }
        return request.getArguments();
    }

    private boolean isLikelyBinary(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".class") || fileName.endsWith(".jar") || fileName.endsWith(".png")
                || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".gif")
                || fileName.endsWith(".zip") || fileName.endsWith(".pdf");
    }

    private CodeHelperToolResult buildResult(String toolName, boolean success, String message, String output, Instant start) {
        return CodeHelperToolResult.builder()
                .toolName(toolName)
                .riskLevel(resolveRiskLevel(toolName))
                .success(success)
                .message(message)
                .output(output)
                .durationMillis(Duration.between(start, Instant.now()).toMillis())
                .build();
    }

    private String resolveRiskLevel(String toolName) {
        if (HIGH_RISK_TOOLS.contains(toolName)) {
            return "HIGH";
        }
        if (MEDIUM_RISK_TOOLS.contains(toolName)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private CodeHelperToolDescriptor descriptor(String toolName, String displayName, String description,
                                                String riskLevel, List<String> argumentNames) {
        return CodeHelperToolDescriptor.builder()
                .toolName(toolName)
                .displayName(displayName)
                .description(description)
                .riskLevel(riskLevel)
                .argumentNames(argumentNames)
                .build();
    }
}
