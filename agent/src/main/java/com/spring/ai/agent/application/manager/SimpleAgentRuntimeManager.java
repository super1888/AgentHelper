package com.spring.ai.agent.application.manager;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.spring.ai.agent.domain.dto.AgentInfoDTO;
import com.spring.ai.agent.domain.dto.SimpleAgentModelBindingDTO;
import com.spring.ai.agent.domain.dto.SimpleAgentVersionConfigDTO;
import com.spring.ai.agent.factory.AgentFactory;
import com.spring.ai.agent.store.SimpleAgentRegistry;
import com.spring.ai.agent.store.SimpleAgentRegistry.StoredSimpleAgent;
import com.spring.ai.common.enums.AgentTypeEnum;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.Agent;
import com.spring.ai.common.repository.enitiy.AgentVersion;
import com.spring.ai.core.application.manager.CoreApplicationManager;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Agent 运行时管理器。
 *
 * <p>根据版本快照恢复运行时 ReactAgent，并缓存到注册表中，
 * 避免每次会话请求都重复初始化模型与工具链。</p>
 */
@Component
public class SimpleAgentRuntimeManager {

    @Resource
    private SimpleAgentRegistry simpleAgentRegistry;

    @Resource
    private SimpleAgentSupportManager simpleAgentSupportManager;

    @Resource
    private AgentFactory agentFactory;

    @Resource
    private CoreApplicationManager coreApplicationManager;

    public ReactAgent getOrCreate(Agent agent, AgentVersion version) {
        StoredSimpleAgent storedSimpleAgent = simpleAgentRegistry.get(version.getId());
        if (storedSimpleAgent != null) {
            return storedSimpleAgent.getReactAgent();
        }

        SimpleAgentVersionConfigDTO config = simpleAgentSupportManager.parseConfig(version.getConfigSnapshotJson());
        SimpleAgentModelBindingDTO modelBinding = config == null ? null : config.getModelBinding();
        AgentInfoDTO agentInfoDTO = AgentInfoDTO.builder()
                .agentId(agent.getId())
                .agentName(config.getAgentName())
                .description(buildDescription(config))
                .instruction(buildInstruction(config))
                .model(resolveChatModel(modelBinding))
                .enableLogging(Boolean.FALSE)
                .build();

        ReactAgent reactAgent = (ReactAgent) agentFactory.createAgent(AgentTypeEnum.REACT, agentInfoDTO);
        simpleAgentRegistry.save(StoredSimpleAgent.builder()
                .versionId(version.getId())
                .agentId(agent.getId())
                .agentName(config.getAgentName())
                .description(agentInfoDTO.getDescription())
                .versionNo(version.getVersionNo())
                .reactAgent(reactAgent)
                .build());
        return reactAgent;
    }

    private org.springframework.ai.chat.model.ChatModel resolveChatModel(SimpleAgentModelBindingDTO modelBinding) {
        if (modelBinding == null || !StringUtils.hasText(modelBinding.getModelCode())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "当前 Agent 版本未绑定模型配置");
        }
        return coreApplicationManager.createChatModel(modelBinding.getModelCode());
    }

    private String buildDescription(SimpleAgentVersionConfigDTO config) {
        if (StringUtils.hasText(config.getDescription())) {
            return config.getDescription().trim();
        }
        if (config.getSelectedCapabilities() == null || config.getSelectedCapabilities().isEmpty()) {
            return "Simple agent created from selected frontend options";
        }
        return "Simple agent with selected capabilities: " + String.join(", ", config.getSelectedCapabilities());
    }

    private String buildInstruction(SimpleAgentVersionConfigDTO config) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(config.getSystemPrompt())) {
            builder.append(config.getSystemPrompt().trim()).append("\n");
        } else {
            builder.append("You are a configurable assistant created from frontend options.\n");
        }
        if (config.getSelectedCapabilities() != null && !config.getSelectedCapabilities().isEmpty()) {
            builder.append("Enabled capabilities: ")
                    .append(String.join(", ", config.getSelectedCapabilities()))
                    .append(".\n");
            builder.append("When answering the user, keep these selected capabilities in mind.\n");
        }
        builder.append("Reply clearly and directly to the user.");
        return builder.toString();
    }
}
