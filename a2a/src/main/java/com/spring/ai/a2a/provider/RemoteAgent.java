package com.spring.ai.a2a.provider;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.spring.ai.common.constants.A2ANameConstants;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件用途：A2A 远程 Agent 调用适配器
 * 核心职责：基于 AgentCardProvider 按 agentCode 动态发现并执行远程 A2A 调用
 */
@Component
public class RemoteAgent {

    private final AgentCardProvider agentCardProvider;
    private final ExecutorService remoteInvokeExecutor = Executors.newCachedThreadPool();

    @Autowired
    public RemoteAgent(@Qualifier("nacosAgentCardProvider") AgentCardProvider agentCardProvider) {
        this.agentCardProvider = agentCardProvider;
    }

    public A2aRemoteAgent callRemoteAgent() {
        return A2aRemoteAgent.builder()
                .name(A2ANameConstants.DATA_ANALYSIS_AGENT)
                .agentCardProvider(agentCardProvider)
                .description("A2A data analysis remote agent")
                .build();
    }

    /**
     * 按指定 AgentCode 调用远程 Agent，并返回执行后的状态数据。
     */
    public Map<String, Object> invokeRemoteAgent(String agentCode, Map<String, Object> payload) {
        return invokeRemoteAgent(agentCode, payload, null);
    }

    /**
     * 按指定 AgentCode 调用远程 Agent，并对单次调用施加超时保护。
     */
    public Map<String, Object> invokeRemoteAgent(String agentCode, Map<String, Object> payload, Integer timeoutMs) {
        if (!StringUtils.hasText(agentCode)) {
            throw new IllegalArgumentException("agentCode is required");
        }
        AgentCardWrapper agentCard = agentCardProvider.getAgentCard(agentCode.trim());
        if (agentCard == null) {
            throw new IllegalStateException("Remote agent card not found: " + agentCode);
        }

        A2aRemoteAgent remoteAgent = A2aRemoteAgent.builder()
                .name(agentCode.trim())
                .agentCardProvider(agentCardProvider)
                .description(agentCard.description())
                .build();
        CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(
                () -> doInvoke(remoteAgent, payload),
                remoteInvokeExecutor
        );
        try {
            if (timeoutMs == null || timeoutMs <= 0) {
                return future.get();
            }
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IllegalStateException("Remote A2A invoke timeout after " + timeoutMs + "ms: " + agentCode, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Remote A2A invoke interrupted: " + agentCode, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CompletionException completionException && completionException.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Remote A2A invoke failed: " + agentCode, cause);
        }
    }

    @PreDestroy
    public void destroy() {
        remoteInvokeExecutor.shutdownNow();
    }

    private Map<String, Object> doInvoke(A2aRemoteAgent remoteAgent, Map<String, Object> payload) {
        try {
            Optional<OverAllState> result = remoteAgent.invoke(payload == null ? Map.of() : payload);
            return result.map(OverAllState::data).orElse(Map.of());
        } catch (GraphRunnerException e) {
            throw new CompletionException(new IllegalStateException("Remote A2A invoke failed: " + e.getMessage(), e));
        }
    }
}
