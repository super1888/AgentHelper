package com.spring.ai.a2a.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import org.junit.jupiter.api.Test;

class RemoteAgentTest {

    private final AgentCardProvider agentCardProvider = mock(AgentCardProvider.class);
    private final RemoteAgent remoteAgent = new RemoteAgent(agentCardProvider);

    @Test
    void callRemoteAgentBuildsNamedRemoteAgent() {
        assertThat(remoteAgent.callRemoteAgent()).isNotNull();
    }

    @Test
    void invokeRemoteAgentRejectsBlankAgentCode() {
        assertThatThrownBy(() -> remoteAgent.invokeRemoteAgent(" ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("agentCode is required");
    }

    @Test
    void invokeRemoteAgentRejectsMissingAgentCard() {
        when(agentCardProvider.getAgentCard("missing-agent")).thenReturn(null);

        assertThatThrownBy(() -> remoteAgent.invokeRemoteAgent(" missing-agent ", null, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Remote agent card not found:  missing-agent ");
    }

    @Test
    void destroyCanBeCalledRepeatedly() {
        remoteAgent.destroy();
        remoteAgent.destroy();
    }
}


