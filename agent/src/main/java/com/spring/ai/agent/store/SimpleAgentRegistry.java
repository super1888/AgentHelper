package com.spring.ai.agent.store;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Agent 运行时缓存。
 *
 * <p>数据库负责持久化，内存仅缓存已经构建过的运行时 Agent，
 * 避免每次会话请求都重新初始化模型与工具链。</p>
 */
@Component
public class SimpleAgentRegistry {

    private final Map<Long, StoredSimpleAgent> agentStore = new ConcurrentHashMap<>();

    public void save(StoredSimpleAgent storedSimpleAgent) {
        agentStore.put(storedSimpleAgent.getVersionId(), storedSimpleAgent);
    }

    /**
     * 按版本主键获取运行时缓存。
     */
    public StoredSimpleAgent get(Long versionId) {
        return agentStore.get(versionId);
    }

    @Getter
    @Builder
    public static class StoredSimpleAgent {

        private Long versionId;

        private Long agentId;

        private String agentName;

        private String description;

        private Integer versionNo;

        private ReactAgent reactAgent;
    }
}
