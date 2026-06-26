package com.spring.ai.codehelper.application.subagent;

import com.spring.ai.codehelper.domain.dto.CodeHelperSubAgentDefinitionDTO;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * codeHelper 子 Agent 注册表。
 * 参考 Claude Code 的子代理分工思想，内置探索、计划、编码和审查四类角色。
 *
 * @author zhouqi
 * @since 2026/6/26
 */
@Component
public class CodeHelperSubAgentRegistry {

    private final Map<String, CodeHelperSubAgentDefinitionDTO> definitions = buildDefinitions().stream()
            .collect(Collectors.toUnmodifiableMap(CodeHelperSubAgentDefinitionDTO::getAgentType, Function.identity()));

    /**
     * 查询所有子 Agent 定义。
     *
     * @return 子 Agent 定义列表
     */
    public List<CodeHelperSubAgentDefinitionDTO> listDefinitions() {
        return definitions.values().stream().toList();
    }

    /**
     * 根据类型获取子 Agent 定义。
     *
     * @param agentType 子 Agent 类型
     * @return 子 Agent 定义
     */
    public CodeHelperSubAgentDefinitionDTO requireDefinition(String agentType) {
        CodeHelperSubAgentDefinitionDTO definition = definitions.get(normalizeAgentType(agentType));
        if (definition == null) {
            throw new IllegalArgumentException("不支持的子 Agent 类型：" + agentType);
        }
        return definition;
    }

    /**
     * 根据任务内容自动选择合适的子 Agent。
     *
     * @param task 任务内容
     * @return 子 Agent 定义
     */
    public CodeHelperSubAgentDefinitionDTO selectDefinition(String task) {
        String normalized = StringUtils.hasText(task) ? task.toLowerCase() : "";
        if (containsAny(normalized, "review", "审查", "检查", "风险", "问题", "bug", "回归")) {
            return requireDefinition("reviewer");
        }
        if (containsAny(normalized, "plan", "方案", "计划", "拆解", "设计", "步骤")) {
            return requireDefinition("planner");
        }
        if (containsAny(normalized, "实现", "开发", "修改", "修复", "编码", "edit", "write", "implement")) {
            return requireDefinition("coder");
        }
        return requireDefinition("explorer");
    }

    private String normalizeAgentType(String agentType) {
        if (!StringUtils.hasText(agentType)) {
            return "explorer";
        }
        return agentType.trim().toLowerCase();
    }

    private boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<CodeHelperSubAgentDefinitionDTO> buildDefinitions() {
        return List.of(
                CodeHelperSubAgentDefinitionDTO.builder()
                        .agentType("explorer")
                        .agentName("探索子 Agent")
                        .description("负责只读代码探索、定位文件、梳理调用链，不直接修改代码。")
                        .systemPrompt("你是探索型子 Agent。只做只读分析，优先使用 list_files、glob、grep、read_file，输出关键文件、证据和结论，不要修改文件。")
                        .allowedTools(List.of("list_files", "glob", "grep", "read_file", "git_status", "git_diff"))
                        .writeAllowed(false)
                        .build(),
                CodeHelperSubAgentDefinitionDTO.builder()
                        .agentType("planner")
                        .agentName("计划子 Agent")
                        .description("负责需求拆解、技术方案和执行步骤，不直接修改代码。")
                        .systemPrompt("你是计划型子 Agent。基于已有上下文输出可执行方案、改动边界、风险点和验证建议，不要直接修改文件。")
                        .allowedTools(List.of("list_files", "glob", "grep", "read_file", "todo_update"))
                        .writeAllowed(false)
                        .build(),
                CodeHelperSubAgentDefinitionDTO.builder()
                        .agentType("coder")
                        .agentName("编码子 Agent")
                        .description("负责给出具体代码改动建议，可生成工具调用计划；高风险和写入操作需要确认。")
                        .systemPrompt("你是编码型子 Agent。先定位最小改动点，再输出修改建议或工具调用计划。写入、编辑、Shell 操作必须要求确认。")
                        .allowedTools(List.of("list_files", "glob", "grep", "read_file", "write_file", "edit_file", "shell", "git_diff"))
                        .writeAllowed(true)
                        .build(),
                CodeHelperSubAgentDefinitionDTO.builder()
                        .agentType("reviewer")
                        .agentName("审查子 Agent")
                        .description("负责代码审查、风险识别、测试缺口和回归影响分析。")
                        .systemPrompt("你是审查型子 Agent。优先指出确定的问题、风险等级、文件位置和验证缺口，不要做泛泛总结，不要修改文件。")
                        .allowedTools(List.of("list_files", "glob", "grep", "read_file", "git_status", "git_diff"))
                        .writeAllowed(false)
                        .build()
        );
    }
}
