package com.spring.ai.agent.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * WebSocket 事件消息体。
 */
@Value
@Builder
public class SimpleAgentWsEvent {

    /**
     * Agent 业务编码。
     */
    String agentId;

    /**
     * 会话业务编码。
     */
    String sessionId;

    /**
     * 任务业务编码。
     */
    String taskId;

    /**
     * Agent 版本主键。
     */
    Long agentVersionId;

    /**
     * Agent 版本号。
     */
    Integer agentVersionNo;

    /**
     * 会话内事件序号。
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
