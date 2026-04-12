package com.spring.ai.agent.provider;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.spring.ai.common.constants.A2ANameConstants;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 使用 AgentCardProvider 发现并调用远程 Agent
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/12
 */
@Component
public class RemoteAgent {

    private final AgentCardProvider agentCardProvider;

    @Autowired
    public RemoteAgent(@Qualifier("nacosAgentCardProvider") AgentCardProvider agentCardProvider) {
        this.agentCardProvider = agentCardProvider;
    }

    public A2aRemoteAgent callRemoteAgent() {
        // 通过 AgentCardProvider 从注册中心发现 Agent
        A2aRemoteAgent remote = A2aRemoteAgent.builder()
                .name(A2ANameConstants.DATA_ANALYSIS_AGENT)
                .agentCardProvider(agentCardProvider)  // 从 Nacos 自动获取 AgentCard
                .description("数据分析远程代理")
                .build();

        return remote;
    }
}
