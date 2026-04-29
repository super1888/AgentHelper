package com.spring.ai.mcp.config;

/**
 * 文件用途：MCP 管理模块常量定义
 * 核心职责：统一管理服务状态、发布状态、接入类型、风险等级和内置服务标识
 */
public final class McpManagementConstants {

    public static final String SERVER_STATUS_ENABLED = "ENABLED";
    public static final String SERVER_STATUS_DISABLED = "DISABLED";

    public static final String PUBLISH_STATUS_DRAFT = "DRAFT";
    public static final String PUBLISH_STATUS_PUBLISHED = "PUBLISHED";
    public static final String PUBLISH_STATUS_OFFLINE = "OFFLINE";

    public static final String SERVER_TYPE_BUILTIN = "BUILTIN";
    public static final String SERVER_TYPE_REMOTE = "REMOTE";

    public static final String TRANSPORT_TYPE_IN_PROCESS = "IN_PROCESS";
    public static final String TRANSPORT_TYPE_HTTP = "HTTP";

    public static final String RISK_LEVEL_LOW = "LOW";
    public static final String RISK_LEVEL_MEDIUM = "MEDIUM";
    public static final String RISK_LEVEL_HIGH = "HIGH";

    public static final String LOG_SOURCE_DEBUG = "DEBUG";
    public static final String LOG_SOURCE_RUNTIME = "RUNTIME";

    public static final String EXECUTE_STATUS_SUCCESS = "SUCCESS";
    public static final String EXECUTE_STATUS_FAILED = "FAILED";

    public static final String BUILTIN_SERVER_DATABASE = "BUILTIN_DATABASE_READONLY";
    public static final String BUILTIN_SERVER_WEATHER = "BUILTIN_WEATHER_HTTP";

    public static final String TOOL_MODE_QUERY = "QUERY";
    public static final String TOOL_MODE_SCHEMA = "SCHEMA";

    private McpManagementConstants() {
    }
}
