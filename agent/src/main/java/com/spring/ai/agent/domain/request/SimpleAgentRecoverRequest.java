package com.spring.ai.agent.domain.request;

import lombok.Data;

/**
 * 失败任务恢复请求。
 */
@Data
public class SimpleAgentRecoverRequest {

    /**
     * 待恢复的任务编码。
     *
     * <p>为空时默认恢复该会话最近一次失败任务。</p>
     */
    private String taskId;
}
