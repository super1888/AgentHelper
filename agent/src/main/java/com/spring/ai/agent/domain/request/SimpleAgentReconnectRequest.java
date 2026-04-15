package com.spring.ai.agent.domain.request;

import lombok.Data;

/**
 * 会话重连请求。
 */
@Data
public class SimpleAgentReconnectRequest {

    /**
     * 客户端最后收到的事件序号。
     */
    private Long lastReceivedEventSequence;
}
