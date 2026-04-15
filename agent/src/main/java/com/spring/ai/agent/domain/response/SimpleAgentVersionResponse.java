package com.spring.ai.agent.domain.response;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Agent 版本响应。
 */
@Value
@Builder
public class SimpleAgentVersionResponse {

    /**
     * 版本主键。
     */
    Long versionId;

    /**
     * 版本号。
     */
    Integer versionNo;

    /**
     * 版本中的 Agent 名称。
     */
    String agentName;

    /**
     * 版本中的 Agent 描述。
     */
    String description;

    /**
     * 版本中的系统提示词。
     */
    String systemPrompt;

    /**
     * 版本中的能力项列表。
     */
    List<String> selectedCapabilities;

    /**
     * 是否已发布。
     */
    Boolean published;

    /**
     * 创建时间戳。
     */
    Long createTime;
}
