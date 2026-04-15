package com.spring.ai.agent.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * Agent 概览响应。
 */
@Value
@Builder
public class SimpleAgentSummaryResponse {

    /**
     * Agent 业务编码。
     */
    String agentId;

    /**
     * Agent 名称。
     */
    String agentName;

    /**
     * Agent 描述。
     */
    String description;

    /**
     * Agent 类型。
     */
    String agentType;

    /**
     * Agent 状态。
     */
    String agentStatus;

    /**
     * 当前最新版本号。
     */
    Integer currentVersionNo;

    /**
     * 当前已发布版本号。
     */
    Integer publishedVersionNo;

    /**
     * 所属用户名。
     */
    String ownerUserName;
}
