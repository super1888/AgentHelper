package com.spring.ai.agent.domain.request;

import lombok.Data;

/**
 * Agent 聊天请求。
 */
@Data
public class SimpleAgentChatRequest {

    /**
     * Agent 业务编码。
     */
    private String agentId;

    /**
     * 会话业务编码。
     */
    private String sessionId;

    /**
     * 用户输入内容。
     */
    private String message;

    /**
     * 客户端最后接收到的事件序号。
     *
     * <p>重连时带上该值，服务端会先补发缺失事件。</p>
     */
    private Long lastReceivedEventSequence;
}
