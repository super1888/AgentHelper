package com.spring.ai.agent.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * WebSocket 推送事件。
 */
@Value
@Builder
public class SimpleAgentWsEvent {

    /**
     * Agent 外部编码。
     */
    String agentId;

    /**
     * 会话外部编码。
     */
    String sessionId;

    /**
     * 任务外部编码。
     */
    String taskId;

    /**
     * 绑定版本主键。
     */
    Long agentVersionId;

    /**
     * 绑定版本号。
     */
    Integer agentVersionNo;

    /**
     * 单会话内严格递增的事件序号。
     */
    Long eventSequence;

    /**
     * 事件类型。
     */
    String event;

    /**
     * 事件数据。
     */
    Object data;

    /**
     * 事件时间戳。
     */
    Long timestamp;
}
