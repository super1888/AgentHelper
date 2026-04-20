package com.spring.ai.prompt.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件用途：承载企业级提示词模板增强配置。
 * 核心功能：统一描述动态渲染、角色约束、流程规则、安全合规、资产复用等扩展能力。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateEnterpriseConfigDTO {

    private RenderingPolicy rendering;

    private RolePolicy rolePolicy;

    private WorkflowPolicy workflowPolicy;

    private SecurityPolicy securityPolicy;

    private AssetPolicy assetPolicy;

    private OutputPolicy outputPolicy;

    private ContextPolicy contextPolicy;

    private FallbackPolicy fallbackPolicy;

    private ObservabilityPolicy observabilityPolicy;

    private IntegrationPolicy integrationPolicy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenderingPolicy {

        private List<String> dynamicVariables;

        private List<String> dataSources;

        private List<ConditionalRule> conditionalBranches;

        private List<LoopRule> loopRenderers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionalRule {

        private String name;

        private String conditionExpression;

        private String trueTemplate;

        private String falseTemplate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoopRule {

        private String listVariable;

        private String itemAlias;

        private String emptyTemplate;

        private String itemTemplate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolePolicy {

        private String agentRole;

        private String dutyScope;

        private List<String> forbiddenActions;

        private String tone;

        private List<String> speechRules;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowPolicy {

        private List<String> workflowStages;

        private List<String> hardRules;

        private List<ToolRule> toolRules;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolRule {

        private String toolCode;

        private String triggerCondition;

        private String parameterSpec;

        private String permissionScope;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityPolicy {

        private List<String> desensitizationRules;

        private List<String> antiInjectionRules;

        private List<String> complianceBlacklist;

        private List<String> permissionTiers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetPolicy {

        private List<String> commonModules;

        private List<String> businessModules;

        private String versionStrategy;

        private String permissionStrategy;

        private List<String> categories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputPolicy {

        private String outputFormat;

        private List<String> requiredFields;

        private Integer maxLength;

        private List<String> channelConstraints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextPolicy {

        private String historyStrategy;

        private List<String> memoryFields;

        private Boolean sessionIsolation;

        private String retentionStrategy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FallbackPolicy {

        private List<String> fallbackMessages;

        private List<String> repeatedRules;

        private List<String> supportedLanguages;

        private String resilienceStrategy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObservabilityPolicy {

        private Boolean traceEnabled;

        private List<String> metricKeys;

        private List<String> logBindingFields;

        private String grayReleaseStrategy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntegrationPolicy {

        private List<String> externalSystems;

        private List<String> parameterBindings;

        private List<String> batchScenarios;

        private String editorMode;
    }
}
