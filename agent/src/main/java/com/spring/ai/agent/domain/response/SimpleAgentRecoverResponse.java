package com.spring.ai.agent.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 失败任务恢复响应。
 */
@Value
@Builder
public class SimpleAgentRecoverResponse {

    /**
     * 会话业务编码。
     */
    String sessionId;

    /**
     * 新恢复任务编码。
     */
    String taskId;

    /**
     * 新恢复任务状态。
     */
    String taskStatus;

    /**
     * 响应说明。
     */
    String message;
}
