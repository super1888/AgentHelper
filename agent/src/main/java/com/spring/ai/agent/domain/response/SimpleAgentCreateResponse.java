package com.spring.ai.agent.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Agent 创建或更新结果。
 */
@Value
@Builder
public class SimpleAgentCreateResponse {

    /**
     * Agent 外部编码。
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
     * 当前版本对应的能力选择。
     */
    List<String> selectedCapabilities;

    /**
     * 当前最新版本号。
     */
    Integer currentVersionNo;

    /**
     * 当前已发布版本号。
     */
    Integer publishedVersionNo;

    /**
     * WebSocket 连接入口。
     */
    String websocketEndpoint;

    /**
     * 会话订阅主题模板。
     */
    String websocketTopic;

    /**
     * 前端发送聊天消息的目标地址。
     */
    String websocketSendDestination;
}
