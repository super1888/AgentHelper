package com.spring.ai.agent.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * 会话重连响应。
 */
@Value
@Builder
public class SimpleAgentReconnectResponse {

    /**
     * 会话信息。
     */
    SimpleAgentSessionResponse session;

    /**
     * 需要补发的事件列表。
     */
    List<SimpleAgentWsEvent> missedEvents;
}
