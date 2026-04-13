package com.spring.ai.agent.store;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * 简单 Agent 内存注册表。
 * 当前只用于演示创建后的 Agent 可以被后续 WebSocket 会话继续复用。
 */
@Component
public class SimpleAgentRegistry {

    private final Map<String, StoredSimpleAgent> agentStore = new ConcurrentHashMap<>();

    public void save(StoredSimpleAgent storedSimpleAgent) {
        agentStore.put(storedSimpleAgent.getAgentId(), storedSimpleAgent);
    }

    public StoredSimpleAgent get(String agentId) {
        StoredSimpleAgent storedSimpleAgent = agentStore.get(agentId);
        if (storedSimpleAgent == null) {
            throw new BusinessException(ErrorCodeEnum.NOT_FOUND, "agent not found: " + agentId);
        }
        return storedSimpleAgent;
    }

    @Getter
    @Builder
    public static class StoredSimpleAgent {

        private String agentId;

        private String agentName;

        private String description;

        private List<String> selectedCapabilities;

        private ReactAgent reactAgent;
    }
}
