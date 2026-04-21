package com.spring.ai.tools.config;

/**
 * 文件用途：工具管理模块常量定义
 * 核心功能：统一管理工具状态、来源、风险等级和日志状态等固定值
 */
public final class ToolManagementConstants {

    public static final String TOOL_STATUS_ENABLED = "ENABLED";
    public static final String TOOL_STATUS_DISABLED = "DISABLED";

    public static final String PUBLISH_STATUS_DRAFT = "DRAFT";
    public static final String PUBLISH_STATUS_PUBLISHED = "PUBLISHED";
    public static final String PUBLISH_STATUS_OFFLINE = "OFFLINE";

    public static final String SOURCE_TYPE_BUILTIN = "BUILTIN";
    public static final String SOURCE_TYPE_API = "API";
    public static final String SOURCE_TYPE_MCP = "MCP";
    public static final String SOURCE_TYPE_AGENT = "AGENT";
    public static final String SOURCE_TYPE_CUSTOM = "CUSTOM";

    public static final String EXECUTION_MODE_SYNC = "SYNC";
    public static final String EXECUTION_MODE_ASYNC = "ASYNC";

    public static final String RISK_LEVEL_LOW = "LOW";
    public static final String RISK_LEVEL_MEDIUM = "MEDIUM";
    public static final String RISK_LEVEL_HIGH = "HIGH";

    public static final String LOG_SOURCE_DEBUG = "DEBUG";
    public static final String LOG_SOURCE_RUNTIME = "RUNTIME";

    public static final String EXECUTE_STATUS_SUCCESS = "SUCCESS";
    public static final String EXECUTE_STATUS_FAILED = "FAILED";

    private ToolManagementConstants() {
    }
}
