package com.spring.ai.agent.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * 创建或更新 Agent 后的响应。
 */
@Value
@Builder
public class SimpleAgentCreateResponse {

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
     * 当前选择的能力项。
     */
    List<String> selectedCapabilities;

    /**
     * 当前版本号。
     */
    Integer currentVersionNo;

    /**
     * 当前已发布版本号。
     */
    Integer publishedVersionNo;

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
