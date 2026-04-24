package com.spring.ai.agent.application.manager;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.spring.ai.agent.domain.dto.AgentInfoDTO;
import com.spring.ai.agent.domain.dto.SimpleAgentModelBindingDTO;
import com.spring.ai.agent.domain.dto.SimpleAgentPromptConfigDTO;
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
import com.spring.ai.prompt.domain.dto.PromptTemplateEnterpriseConfigDTO;
import jakarta.annotation.Resource;
import java.util.List;
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
        /**
         * 将提示词模板中声明的企业策略转换为运行时 instruction，避免配置只保存在快照中却不参与执行。
         */
        appendEnterprisePolicies(builder, config.getPromptConfig());
        builder.append("Reply clearly and directly to the user.");
        return builder.toString();
    }

    /**
     * 将提示词模板中的企业策略显式拼入运行时 instruction，避免配置只落库不生效。
     */
    private void appendEnterprisePolicies(StringBuilder builder, SimpleAgentPromptConfigDTO promptConfig) {
        if (promptConfig == null || promptConfig.getEnterpriseConfig() == null) {
            return;
        }
        PromptTemplateEnterpriseConfigDTO enterpriseConfig = promptConfig.getEnterpriseConfig();
        appendRolePolicy(builder, enterpriseConfig.getRolePolicy());
        appendWorkflowPolicy(builder, enterpriseConfig.getWorkflowPolicy());
        appendSecurityPolicy(builder, enterpriseConfig.getSecurityPolicy());
        appendAssetPolicy(builder, enterpriseConfig.getAssetPolicy());
        appendOutputPolicy(builder, enterpriseConfig.getOutputPolicy());
        appendContextPolicy(builder, enterpriseConfig.getContextPolicy());
        appendFallbackPolicy(builder, enterpriseConfig.getFallbackPolicy());
        appendObservabilityPolicy(builder, enterpriseConfig.getObservabilityPolicy());
        appendIntegrationPolicy(builder, enterpriseConfig.getIntegrationPolicy());
    }

    private void appendRolePolicy(StringBuilder builder, PromptTemplateEnterpriseConfigDTO.RolePolicy rolePolicy) {
        if (rolePolicy == null) {
            return;
        }
        if (StringUtils.hasText(rolePolicy.getAgentRole())) {
            builder.append("Role: ").append(rolePolicy.getAgentRole().trim()).append(".\n");
        }
        if (StringUtils.hasText(rolePolicy.getDutyScope())) {
            builder.append("Duty scope: ").append(rolePolicy.getDutyScope().trim()).append(".\n");
        }
        appendListLine(builder, "Forbidden actions", rolePolicy.getForbiddenActions());
        if (StringUtils.hasText(rolePolicy.getTone())) {
            builder.append("Preferred tone: ").append(rolePolicy.getTone().trim()).append(".\n");
        }
        appendListLine(builder, "Speech rules", rolePolicy.getSpeechRules());
    }

    private void appendWorkflowPolicy(StringBuilder builder, PromptTemplateEnterpriseConfigDTO.WorkflowPolicy workflowPolicy) {
        if (workflowPolicy == null) {
            return;
        }
        appendListLine(builder, "Workflow stages", workflowPolicy.getWorkflowStages());
        appendListLine(builder, "Hard rules", workflowPolicy.getHardRules());
        if (workflowPolicy.getToolRules() == null || workflowPolicy.getToolRules().isEmpty()) {
            return;
        }
        /**
         * 工具规则需要按条展开，否则模型看不到某个工具的触发条件和权限边界。
         */
        for (PromptTemplateEnterpriseConfigDTO.ToolRule toolRule : workflowPolicy.getToolRules()) {
            if (toolRule == null || !StringUtils.hasText(toolRule.getToolCode())) {
                continue;
            }
            builder.append("Tool rule for ")
                    .append(toolRule.getToolCode().trim())
                    .append(": ");
            appendInlineField(builder, "trigger", toolRule.getTriggerCondition());
            appendInlineField(builder, "parameterSpec", toolRule.getParameterSpec());
            appendInlineField(builder, "permissionScope", toolRule.getPermissionScope());
            builder.append("\n");
        }
    }

    private void appendSecurityPolicy(StringBuilder builder, PromptTemplateEnterpriseConfigDTO.SecurityPolicy securityPolicy) {
        if (securityPolicy == null) {
            return;
        }
        appendListLine(builder, "Desensitization rules", securityPolicy.getDesensitizationRules());
        appendListLine(builder, "Anti-injection rules", securityPolicy.getAntiInjectionRules());
        appendListLine(builder, "Compliance blacklist", securityPolicy.getComplianceBlacklist());
        appendListLine(builder, "Permission tiers", securityPolicy.getPermissionTiers());
    }

    private void appendAssetPolicy(StringBuilder builder, PromptTemplateEnterpriseConfigDTO.AssetPolicy assetPolicy) {
        if (assetPolicy == null) {
            return;
        }
        appendListLine(builder, "Common modules", assetPolicy.getCommonModules());
        appendListLine(builder, "Business modules", assetPolicy.getBusinessModules());
        if (StringUtils.hasText(assetPolicy.getVersionStrategy())) {
            builder.append("Version strategy: ").append(assetPolicy.getVersionStrategy().trim()).append(".\n");
        }
        if (StringUtils.hasText(assetPolicy.getPermissionStrategy())) {
            builder.append("Asset permission strategy: ").append(assetPolicy.getPermissionStrategy().trim()).append(".\n");
        }
        appendListLine(builder, "Asset categories", assetPolicy.getCategories());
    }

    private void appendOutputPolicy(StringBuilder builder, PromptTemplateEnterpriseConfigDTO.OutputPolicy outputPolicy) {
        if (outputPolicy == null) {
            return;
        }
        if (StringUtils.hasText(outputPolicy.getOutputFormat())) {
            builder.append("Output format: ").append(outputPolicy.getOutputFormat().trim()).append(".\n");
        }
        appendListLine(builder, "Required output fields", outputPolicy.getRequiredFields());
        if (outputPolicy.getMaxLength() != null && outputPolicy.getMaxLength() > 0) {
            builder.append("Maximum output length: ").append(outputPolicy.getMaxLength()).append(".\n");
        }
        appendListLine(builder, "Channel constraints", outputPolicy.getChannelConstraints());
    }

    private void appendContextPolicy(StringBuilder builder, PromptTemplateEnterpriseConfigDTO.ContextPolicy contextPolicy) {
        if (contextPolicy == null) {
            return;
        }
        if (StringUtils.hasText(contextPolicy.getHistoryStrategy())) {
            builder.append("History strategy: ").append(contextPolicy.getHistoryStrategy().trim()).append(".\n");
        }
        appendListLine(builder, "Memory fields", contextPolicy.getMemoryFields());
        if (contextPolicy.getSessionIsolation() != null) {
            builder.append("Session isolation: ")
                    .append(Boolean.TRUE.equals(contextPolicy.getSessionIsolation()) ? "enabled" : "disabled")
                    .append(".\n");
        }
        if (StringUtils.hasText(contextPolicy.getRetentionStrategy())) {
            builder.append("Retention strategy: ").append(contextPolicy.getRetentionStrategy().trim()).append(".\n");
        }
    }

    private void appendFallbackPolicy(StringBuilder builder, PromptTemplateEnterpriseConfigDTO.FallbackPolicy fallbackPolicy) {
        if (fallbackPolicy == null) {
            return;
        }
        appendListLine(builder, "Fallback messages", fallbackPolicy.getFallbackMessages());
        appendListLine(builder, "Repeated request rules", fallbackPolicy.getRepeatedRules());
        appendListLine(builder, "Supported languages", fallbackPolicy.getSupportedLanguages());
        if (StringUtils.hasText(fallbackPolicy.getResilienceStrategy())) {
            builder.append("Resilience strategy: ").append(fallbackPolicy.getResilienceStrategy().trim()).append(".\n");
        }
    }

    private void appendObservabilityPolicy(StringBuilder builder, PromptTemplateEnterpriseConfigDTO.ObservabilityPolicy observabilityPolicy) {
        if (observabilityPolicy == null) {
            return;
        }
        if (observabilityPolicy.getTraceEnabled() != null) {
            builder.append("Trace enabled: ")
                    .append(Boolean.TRUE.equals(observabilityPolicy.getTraceEnabled()) ? "true" : "false")
                    .append(".\n");
        }
        appendListLine(builder, "Metric keys", observabilityPolicy.getMetricKeys());
        appendListLine(builder, "Log binding fields", observabilityPolicy.getLogBindingFields());
        if (StringUtils.hasText(observabilityPolicy.getGrayReleaseStrategy())) {
            builder.append("Gray release strategy: ").append(observabilityPolicy.getGrayReleaseStrategy().trim()).append(".\n");
        }
    }

    private void appendIntegrationPolicy(StringBuilder builder, PromptTemplateEnterpriseConfigDTO.IntegrationPolicy integrationPolicy) {
        if (integrationPolicy == null) {
            return;
        }
        appendListLine(builder, "External systems", integrationPolicy.getExternalSystems());
        appendListLine(builder, "Parameter bindings", integrationPolicy.getParameterBindings());
        appendListLine(builder, "Batch scenarios", integrationPolicy.getBatchScenarios());
        if (StringUtils.hasText(integrationPolicy.getEditorMode())) {
            builder.append("Editor mode: ").append(integrationPolicy.getEditorMode().trim()).append(".\n");
        }
    }

    private void appendListLine(StringBuilder builder, String label, List<String> items) {
        if (items == null) {
            return;
        }
        /**
         * 统一过滤空白项，避免把空字符串规则拼进 instruction 干扰模型判断。
         */
        List<String> normalizedItems = items.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (normalizedItems.isEmpty()) {
            return;
        }
        builder.append(label)
                .append(": ")
                .append(String.join("; ", normalizedItems))
                .append(".\n");
    }

    private void appendInlineField(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append(label)
                .append("=")
                .append(value.trim())
                .append("; ");
    }
}
