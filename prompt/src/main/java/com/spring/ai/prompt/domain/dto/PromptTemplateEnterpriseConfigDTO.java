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

    // 渲染策略：控制模板如何动态渲染
    private RenderingPolicy rendering;

    // 角色策略：定义AI助手的角色和行为准则
    private RolePolicy rolePolicy;

    // 工作流策略：管理提示词处理流程的各个阶段
    private WorkflowPolicy workflowPolicy;

    // 安全策略：确保生成内容的安全性和合规性
    private SecurityPolicy securityPolicy;

    // 资产策略：管理可复用的组件和模块
    private AssetPolicy assetPolicy;

    // 输出策略：控制输出的格式和约束
    private OutputPolicy outputPolicy;

    // 上下文策略：管理对话历史和上下文信息
    private ContextPolicy contextPolicy;

    // 降级策略：处理系统异常和边界情况
    private FallbackPolicy fallbackPolicy;

    // 可观测性策略：监控和追踪系统行为
    private ObservabilityPolicy observabilityPolicy;

    // 集成策略：与其他系统的接口和交互方式
    private IntegrationPolicy integrationPolicy;

    /**
     * 渲染策略内部类
     * 定义了模板渲染时的各种规则和配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenderingPolicy {

        // 动态变量列表：在模板中可以动态替换的变量
        private List<String> dynamicVariables;

        // 数据源列表：模板渲染时可用的数据源
        private List<String> dataSources;

        // 条件分支规则：控制模板的条件渲染逻辑
        private List<ConditionalRule> conditionalBranches;

        // 循环渲染规则：处理列表数据的循环渲染
        private List<LoopRule> loopRenderers;
    }

    /**
     * 条件规则内部类
     * 定义模板中的条件渲染逻辑
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionalRule {

        // 规则名称：标识条件规则的唯一名称
        private String name;

        // 条件表达式：判断条件的逻辑表达式
        private String conditionExpression;

        // 条件为真时的模板内容
        private String trueTemplate;

        // 条件为假时的模板内容
        private String falseTemplate;
    }

    /**
     * 循环规则内部类
     * 定义模板中的循环渲染逻辑
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoopRule {

        // 列表变量名：要遍历的列表变量
        private String listVariable;

        // 项目别名：循环中当前项的别名
        private String itemAlias;

        // 空列表时的模板内容
        private String emptyTemplate;

        // 列表项的模板内容
        private String itemTemplate;
    }

    /**
     * 角色策略内部类
     * 定义AI助手的角色和行为准则
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolePolicy {

        // AI代理的角色定义
        private String agentRole;

        // 职责范围：AI代理可以处理的任务范围
        private String dutyScope;

        // 禁止动作列表：AI代理不能执行的操作
        private List<String> forbiddenActions;

        // 语调风格：AI回复的语气风格
        private String tone;

        // 说话规则：AI回复需要遵守的语言规则
        private List<String> speechRules;
    }

    /**
     * 工作流策略内部类
     * 管理提示词处理流程的各个阶段
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowPolicy {

        // 工作流阶段列表：定义流程的各个阶段
        private List<String> workflowStages;

        // 硬性规则：必须遵守的规则
        private List<String> hardRules;

        // 工具规则：使用工具的规则配置
        private List<ToolRule> toolRules;
    }

    /**
     * 工具规则内部类
     * 定义工具使用时的规则和限制
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolRule {

        // 工具代码：唯一标识工具的代码
        private String toolCode;

        // 触发条件：使用该工具的条件
        private String triggerCondition;

        // 参数规格：工具参数的规范要求
        private String parameterSpec;

        // 权限范围：使用该工具所需的权限
        private String permissionScope;
    }

    /**
     * 安全策略内部类
     * 确保生成内容的安全性和合规性
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityPolicy {

        // 脱敏规则：处理敏感数据的规则
        private List<String> desensitizationRules;

        // 防注入规则：防止代码注入的规则
        private List<String> antiInjectionRules;

        // 合规黑名单：禁止使用的内容列表
        private List<String> complianceBlacklist;

        // 权限层级：访问控制的权限级别
        private List<String> permissionTiers;
    }

    /**
     * 资产策略内部类
     * 管理可复用的组件和模块
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetPolicy {

        // 通用模块列表：可复用的通用功能模块
        private List<String> commonModules;

        // 业务模块列表：特定业务的功能模块
        private List<String> businessModules;

        // 版本策略：模块版本管理的策略
        private String versionStrategy;

        // 权限策略：模块使用的权限控制
        private String permissionStrategy;

        // 模块分类：按类别组织的模块列表
        private List<String> categories;
    }

    /**
     * 输出策略内部类
     * 控制输出的格式和约束
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputPolicy {

        // 输出格式：定义输出的格式类型
        private String outputFormat;

        // 必填字段：输出中必须包含的字段
        private List<String> requiredFields;

        // 最大长度：输出内容的最大长度限制
        private Integer maxLength;

        // 渠道约束：针对不同输出渠道的特殊要求
        private List<String> channelConstraints;
    }

    /**
     * 上下文策略内部类
     * 管理对话历史和上下文信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextPolicy {

        // 历史策略：管理对话历史的保存和清理
        private String historyStrategy;

        // 记忆字段：需要长期记忆的上下文字段
        private List<String> memoryFields;

        // 会话隔离：是否隔离不同会话的上下文
        private Boolean sessionIsolation;

        // 保留策略：上下文信息的保留规则
        private String retentionStrategy;
    }

    /**
     * 降级策略内部类
     * 处理系统异常和边界情况
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FallbackPolicy {

        // 降级消息：系统出错时的回复消息
        private List<String> fallbackMessages;

        // 重复规则：处理重复请求的规则
        private List<String> repeatedRules;

        // 支持语言：系统支持的语言列表
        private List<String> supportedLanguages;

        // 韧性策略：系统容错和恢复的策略
        private String resilienceStrategy;
    }

    /**
     * 可观测性策略内部类
     * 监控和追踪系统行为
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObservabilityPolicy {

        // 是否启用链路追踪
        private Boolean traceEnabled;

        // 监控指标键值列表
        private List<String> metricKeys;

        // 日志绑定字段：需要记录到日志的字段
        private List<String> logBindingFields;

        // 灰度发布策略：新功能发布的策略
        private String grayReleaseStrategy;
    }

    /**
     * 集成策略内部类
     * 与其他系统的接口和交互方式
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntegrationPolicy {

        // 外部系统列表：需要集成的外部系统
        private List<String> externalSystems;

        // 参数绑定：与外部系统交互的参数绑定规则
        private List<String> parameterBindings;

        // 批处理场景：支持批量处理的场景
        private List<String> batchScenarios;

        // 编辑器模式：与外部系统交互时的编辑器模式
        private String editorMode;
    }
}
