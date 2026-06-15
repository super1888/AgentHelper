package com.spring.ai.codehelper.application.manager;

import com.spring.ai.codehelper.domain.dto.CodeHelperSessionDTO;
import com.spring.ai.tools.codehelper.CodeHelperToolDescriptor;
import com.spring.ai.tools.codehelper.CodeHelperWorkspaceToolExecutor;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * codeHelper Prompt 组装器。
 *
 * <p>按身份、工作模式、安全策略、工具清单、工作区上下文、上下文摘要和 JSON 输出协议分层组装系统提示词。</p>
 */
@Component
public class CodeHelperPromptManager {

    @Resource
    private CodeHelperWorkspaceToolExecutor workspaceToolExecutor;

    /**
     * 构建编程助手系统提示词。
     */
    public String buildSystemPrompt(CodeHelperSessionDTO session) {
        return String.join("\n\n",
                identitySection(),
                workModeSection(),
                safetySection(),
                toolSection(),
                workspaceSection(session),
                memorySection(session),
                outputSection()
        );
    }

    private String identitySection() {
        return """
                # 身份
                你是 codeHelper，一个面向 Java 代码库工作的编程助手。
                你需要通过分析、修改、验证、总结四个阶段完成用户目标。
                """.trim();
    }

    private String workModeSection() {
        return """
                # 工作模式
                采用 ReAct 工作方式：先分析当前状态，再决定是否调用工具，工具结果返回后继续分析。
                你不能直接声称已经修改文件，必须通过工具调用结果确认。
                """.trim();
    }

    private String safetySection() {
        return """
                # 安全策略
                所有路径必须限制在工作区内。
                读取、搜索、列目录属于低风险；写入和编辑属于中风险；Shell、Git 提交和推送属于高风险。
                高风险操作必须要求用户确认，不允许删除目录、格式化磁盘、系统级破坏命令。
                """.trim();
    }

    private String toolSection() {
        List<CodeHelperToolDescriptor> tools = workspaceToolExecutor.listTools();
        String toolText = tools.stream()
                .map(tool -> "- " + tool.getToolName() + "：" + tool.getDescription() + "，风险=" + tool.getRiskLevel()
                        + "，参数=" + tool.getArgumentNames())
                .collect(Collectors.joining("\n"));
        return "# 工具清单\n" + toolText;
    }

    private String workspaceSection(CodeHelperSessionDTO session) {
        return """
                # 工作区上下文
                工作区：%s
                项目：%s
                分支：%s
                当前任务：%s
                """.formatted(
                session.getWorkspacePath(),
                session.getProjectName(),
                session.getBranchName(),
                session.getTaskDescription()
        ).trim();
    }

    private String memorySection(CodeHelperSessionDTO session) {
        return """
                # 当前摘要
                %s
                """.formatted(session.getSummary()).trim();
    }

    private String outputSection() {
        return """
                # 输出协议
                你必须只输出一个 JSON 对象，不要输出 Markdown，不要输出代码块。
                JSON 结构如下：
                {
                  "assistantReply": "你对用户的简短说明",
                  "requireConfirmation": false,
                  "toolCalls": [
                    {
                      "toolName": "grep",
                      "arguments": {"keyword": "Controller"}
                    }
                  ]
                }
                如果不需要工具，toolCalls 返回空数组。
                如果需要 shell、git_status、git_diff 等高风险工具，requireConfirmation 必须为 true。
                """.trim();
    }
}
