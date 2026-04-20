package com.spring.ai.prompt.config;

import java.util.Set;

/**
 * 文件用途：集中维护提示词模板模块的常量定义。
 * 核心功能：统一约束模板类型、来源类型、状态、变量规则和文件限制。
 */
public final class PromptTemplateConstants {

    public static final String TEMPLATE_TYPE_SYSTEM = "SYSTEM_PROMPT_TEMPLATE";
    public static final String BINDING_TYPE_TEMPLATE = "TEMPLATE";
    public static final String BINDING_TYPE_CUSTOM = "CUSTOM";
    public static final String SOURCE_TYPE_INLINE = "INLINE_TEXT";
    public static final String SOURCE_TYPE_FILE = "FILE_PATH";
    public static final String TEMPLATE_STATUS_ENABLED = "ENABLED";
    public static final String TEMPLATE_STATUS_DISABLED = "DISABLED";
    public static final int MAX_TEMPLATE_LENGTH = 20000;
    public static final long MAX_FILE_SIZE = 64 * 1024L;
    public static final String TEMPLATE_VARIABLE_PATTERN =
            "\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_\\.]*)\\s*}}|\\{([a-zA-Z][a-zA-Z0-9_\\.]*)}";
    public static final String TEMPLATE_VARIABLE_RULE = "{{variableName}} / {variableName}";
    public static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(".txt", ".md", ".prompt", ".vm", ".ftl");

    private PromptTemplateConstants() {
    }
}
