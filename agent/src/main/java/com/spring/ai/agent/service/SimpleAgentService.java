package com.spring.ai.agent.service;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.spring.ai.agent.domian.dto.AgentInfoDTO;
import com.spring.ai.agent.domain.request.SimpleAgentCreateRequest;
import com.spring.ai.agent.domain.response.SimpleAgentCreateResponse;
import com.spring.ai.agent.factory.AgentFactory;
import com.spring.ai.agent.store.SimpleAgentRegistry;
import com.spring.ai.agent.store.SimpleAgentRegistry.StoredSimpleAgent;
import com.spring.ai.common.enums.AgentTypeEnum;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.core.facotry.GetChatModel;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 简单 Agent 创建服务。
 * 当前实现使用内存注册表保存 Agent，便于快速验证创建和聊天闭环。
 */
@Service
public class SimpleAgentService {

    @Resource
    private AgentFactory agentFactory;

    @Resource
    private GetChatModel getChatModel;

    @Resource
    private SimpleAgentRegistry simpleAgentRegistry;

    public SimpleAgentCreateResponse createAgent(SimpleAgentCreateRequest request) {
        validateCreateRequest(request);

        String agentId = UUID.randomUUID().toString();
        List<String> capabilities = request.getSelectedCapabilities() == null
                ? List.of()
                : new ArrayList<>(request.getSelectedCapabilities());

        AgentInfoDTO agentInfoDTO = AgentInfoDTO.builder()
                .agentId(System.currentTimeMillis())
                .agentName(request.getAgentName().trim())
                .description(buildDescription(request.getDescription(), capabilities))
                .instruction(buildInstruction(request.getSystemPrompt(), capabilities))
                .model(getChatModel.creatDashScopeChatModel())
                .enableLogging(Boolean.FALSE)
                .build();

        ReactAgent reactAgent = (ReactAgent) agentFactory.createAgent(AgentTypeEnum.REACT, agentInfoDTO);
        simpleAgentRegistry.save(StoredSimpleAgent.builder()
                .agentId(agentId)
                .agentName(request.getAgentName().trim())
                .description(agentInfoDTO.getDescription())
                .selectedCapabilities(capabilities)
                .reactAgent(reactAgent)
                .build());

        return SimpleAgentCreateResponse.builder()
                .agentId(agentId)
                .agentName(request.getAgentName().trim())
                .description(agentInfoDTO.getDescription())
                .selectedCapabilities(capabilities)
                .websocketEndpoint("/ws")
                .websocketTopic("/topic/session/{sessionId}")
                .websocketSendDestination("/app/agent/chat")
                .build();
    }

    private void validateCreateRequest(SimpleAgentCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getAgentName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "agentName must not be blank");
        }
    }

    private String buildDescription(String description, List<String> capabilities) {
        if (StringUtils.hasText(description)) {
            return description.trim();
        }
        if (CollectionUtils.isEmpty(capabilities)) {
            return "Simple agent created from selected frontend options";
        }
        return "Simple agent with selected capabilities: " + String.join(", ", capabilities);
    }

    private String buildInstruction(String systemPrompt, List<String> capabilities) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(systemPrompt)) {
            builder.append(systemPrompt.trim()).append("\n");
        } else {
            builder.append("You are a configurable assistant created from frontend options.\n");
        }
        if (!CollectionUtils.isEmpty(capabilities)) {
            builder.append("Enabled capabilities: ").append(String.join(", ", capabilities)).append(".\n");
            builder.append("When answering the user, keep these selected capabilities in mind.\n");
        }
        builder.append("Reply clearly and directly to the user.");
        return builder.toString();
    }
}
