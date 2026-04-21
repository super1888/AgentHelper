package com.spring.ai.hooks.config;

/**
 * 文件用途：Hook 管理模块常量定义
 * 核心职责：统一管理 Hook 状态、发布、日志和版本相关枚举值
 */
public final class HookManagementConstants {

    public static final String HOOK_STATUS_ENABLED = "ENABLED";
    public static final String HOOK_STATUS_DISABLED = "DISABLED";

    public static final String PUBLISH_STATUS_DRAFT = "DRAFT";
    public static final String PUBLISH_STATUS_PUBLISHED = "PUBLISHED";
    public static final String PUBLISH_STATUS_OFFLINE = "OFFLINE";

    public static final String VERSION_STATUS_CURRENT = "CURRENT";
    public static final String VERSION_STATUS_HISTORY = "HISTORY";
    public static final String VERSION_STATUS_ROLLBACK = "ROLLBACK";

    public static final String RISK_LEVEL_LOW = "LOW";
    public static final String RISK_LEVEL_MEDIUM = "MEDIUM";
    public static final String RISK_LEVEL_HIGH = "HIGH";
    public static final String RISK_LEVEL_CRITICAL = "CRITICAL";

    public static final String SOURCE_TYPE_DEBUG = "DEBUG";
    public static final String SOURCE_TYPE_TEST = "TEST";
    public static final String SOURCE_TYPE_RUNTIME = "RUNTIME";

    public static final String EXECUTE_STATUS_SUCCESS = "SUCCESS";
    public static final String EXECUTE_STATUS_FAILED = "FAILED";

    private HookManagementConstants() {
    }
}
