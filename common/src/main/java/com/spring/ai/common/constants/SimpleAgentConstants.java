package com.spring.ai.common.constants;

/**
 * Simple Agent 模块常量。
 *
 * <p>统一维护 Agent、会话、任务等状态常量，
 * 避免业务代码中散落硬编码。</p>
 */
public final class SimpleAgentConstants {

    private SimpleAgentConstants() {
    }

    /**
     * 当前支持的 Agent 类型。
     */
    public static final String AGENT_TYPE_REACT = "REACT";

    /**
     * Agent 草稿态。
     */
    public static final String AGENT_STATUS_DRAFT = "DRAFT";

    /**
     * Agent 已发布。
     */
    public static final String AGENT_STATUS_PUBLISHED = "PUBLISHED";

    /**
     * Agent 已禁用。
     */
    public static final String AGENT_STATUS_DISABLED = "DISABLED";

    /**
     * 会话活跃中。
     */
    public static final String SESSION_STATUS_ACTIVE = "ACTIVE";

    /**
     * 会话已关闭。
     */
    public static final String SESSION_STATUS_CLOSED = "CLOSED";

    /**
     * 会话执行失败。
     */
    public static final String SESSION_STATUS_FAILED = "FAILED";

    /**
     * 连接已建立。
     */
    public static final String CONNECTION_STATUS_CONNECTED = "CONNECTED";

    /**
     * 连接已断开。
     */
    public static final String CONNECTION_STATUS_DISCONNECTED = "DISCONNECTED";

    /**
     * 任务待执行。
     */
    public static final String TASK_STATUS_PENDING = "PENDING";

    /**
     * 任务执行中。
     */
    public static final String TASK_STATUS_RUNNING = "RUNNING";

    /**
     * 任务执行成功。
     */
    public static final String TASK_STATUS_SUCCESS = "SUCCESS";

    /**
     * 任务执行失败。
     */
    public static final String TASK_STATUS_FAILED = "FAILED";
}
