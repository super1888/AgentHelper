package com.spring.ai.prompt.config;

import java.util.Set;

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
    public static final String TEMPLATE_VARIABLE_PATTERN = "\\{\\{\\s*([a-zA-Z][a-zA-Z0-9_]*)\\s*}}";
    public static final String TEMPLATE_VARIABLE_RULE = "{{variableName}}";
    public static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(".txt", ".md", ".prompt", ".vm", ".ftl");

    private PromptTemplateConstants() {
    }
}
