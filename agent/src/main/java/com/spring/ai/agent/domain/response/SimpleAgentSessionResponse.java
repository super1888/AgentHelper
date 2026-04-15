package com.spring.ai.agent.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 会话响应。
 */
@Value
@Builder
public class SimpleAgentSessionResponse {

    /**
     * 会话业务编码。
     */
    String sessionId;

    /**
     * Agent 业务编码。
     */
    String agentId;

    /**
     * 绑定的版本号。
     */
    Integer agentVersionNo;

    /**
     * 绑定的版本主键。
     */
    Long agentVersionId;

    /**
     * 会话状态。
     */
    String sessionStatus;

    /**
     * 当前连接状态。
     */
    String connectionStatus;

    /**
     * 最近一次事件序号。
     */
    Long lastEventSequence;

    /**
     * WebSocket 接入端点。
     */
    String websocketEndpoint;

    /**
     * WebSocket 订阅主题模板。
     */
    String websocketTopic;

    /**
     * WebSocket 发送目标地址。
     */
    String websocketSendDestination;
}
