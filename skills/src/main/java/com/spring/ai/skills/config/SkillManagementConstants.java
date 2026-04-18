package com.spring.ai.skills.config;

public final class SkillManagementConstants {

    public static final String SKILL_STATUS_ENABLED = "ENABLED";
    public static final String SKILL_STATUS_DISABLED = "DISABLED";

    public static final String PUBLISH_STATUS_DRAFT = "DRAFT";
    public static final String PUBLISH_STATUS_TESTING = "TESTING";
    public static final String PUBLISH_STATUS_PRE_RELEASE = "PRE_RELEASE";
    public static final String PUBLISH_STATUS_PUBLISHED = "PUBLISHED";
    public static final String PUBLISH_STATUS_OFFLINE = "OFFLINE";

    public static final String VERSION_MODE_MANUAL = "MANUAL";
    public static final String VERSION_MODE_AUTO = "AUTO";

    public static final String VERSION_STATUS_CURRENT = "CURRENT";
    public static final String VERSION_STATUS_HISTORY = "HISTORY";
    public static final String VERSION_STATUS_ROLLBACK = "ROLLBACK";

    public static final String DEBUG_STATUS_MATCHED = "MATCHED";
    public static final String DEBUG_STATUS_BLOCKED = "BLOCKED";
    public static final String DEBUG_STATUS_FAILED = "FAILED";

    public static final String LOG_SOURCE_DEBUG = "DEBUG";
    public static final String LOG_SOURCE_TEST = "TEST";
    public static final String LOG_SOURCE_RUNTIME = "RUNTIME";

    private SkillManagementConstants() {
    }
}
