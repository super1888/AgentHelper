package com.spring.ai.hooks.factory;

import static com.spring.ai.common.utils.BaseUtils.getOrDefault;

import com.alibaba.cloud.ai.graph.agent.hook.TokenCounter;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.spring.ai.hooks.domain.dto.HumanInTheLoopHookDTO;
import com.spring.ai.hooks.domain.dto.ModelCallLimitHookDTO;
import com.spring.ai.hooks.domain.dto.PIIDetectionHookDTO;
import com.spring.ai.hooks.domain.dto.SummarizationHookDTO;
import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/1
 */
public class HookFactory {

    /**
     * 当接近 token 限制时自动压缩对话历史。
     * <p>
     * 适用场景：
     * <p>
     * 超出上下文窗口的长期对话； 具有大量历史记录的多轮对话； 需要保留完整对话上下文的应用程序。
     *
     * @param summarizationHookDTO
     * @return
     */
    public SummarizationHook creatSummarizationHook(SummarizationHookDTO summarizationHookDTO) {
        if (summarizationHookDTO == null) {
            throw new IllegalArgumentException("SummarizationHookDTO 不能为空");
        }

        // 默认值
        ChatModel model = summarizationHookDTO.getChatModel();
        Integer maxTokens = getOrDefault(summarizationHookDTO.getMaxTokens(), 4000);
        Integer msgToKeep = getOrDefault(summarizationHookDTO.getCount(), 20);
        String prompt = getOrDefault(summarizationHookDTO.getPrompt(),
                "请对以下对话历史进行简洁总结，保留关键信息");
        String prefix = getOrDefault(summarizationHookDTO.getPrefix(), "【对话总结】");
        TokenCounter counter = summarizationHookDTO.getCounter();
        Boolean keepFirst = getOrDefault(summarizationHookDTO.getKeep(), Boolean.TRUE);

        return SummarizationHook.builder()
                .model(model)
                .maxTokensBeforeSummary(maxTokens)
                .messagesToKeep(msgToKeep)
                .summaryPrompt(prompt)
                .summaryPrefix(prefix)
                .tokenCounter(counter)
                .keepFirstUserMessage(keepFirst)
                .build();
    }

    /**
     * HumanInTheLoopHook（HITL，人在回路）是 Spring AI Alibaba 提供的人工介入钩子， 用于让 Agent 在执行敏感 / 高风险工具（如 SQL 执行、文件写入、数据删除）前自动暂停， 等待人工审批（批准 / 拒绝 /
     * 编辑）后再恢复执行，保障生产环境安全、可审计。
     *
     * @param humanInTheLoopHookDTO
     * @return
     */
    public HumanInTheLoopHook creatHumanInTheLoopHook(HumanInTheLoopHookDTO humanInTheLoopHookDTO) {
        if (humanInTheLoopHookDTO == null) {
            throw new IllegalArgumentException("humanInTheLoopHookDTO 不能为空");
        }

        // 1. 处理默认值
        Boolean enabled = getOrDefault(humanInTheLoopHookDTO.getEnabled(), Boolean.TRUE);
        // 未启用则返回空（不添加钩子）
        if (!enabled) {
            return null;
        }

        String defaultDesc = getOrDefault(humanInTheLoopHookDTO.getDefaultApprovalDesc(),
                "该操作需要人工审批确认");
        Map<String, ToolConfig> approvalOn = getOrDefault(humanInTheLoopHookDTO.getApprovalOn(),
                new HashMap<>());

        // 2. 构建官方 HumanInTheLoopHook
        HumanInTheLoopHook.Builder builder = HumanInTheLoopHook.builder();
        // 遍历配置，添加需要审批的工具
        approvalOn.forEach((toolName, toolConfig) -> {
            // 工具配置为空则使用默认描述
            ToolConfig finalConfig = toolConfig != null ? toolConfig :
                    ToolConfig.builder().description(defaultDesc).build();
            builder.approvalOn(toolName, finalConfig);
        });

        return builder.build();
    }


    /**
     * ModelCallLimitHook 是 Alibaba Cloud AI Graph 内置的模型调用次数限制钩子，用于防止 Agent 无限循环、控制推理成本、限制单 轮 / 会话 级模型调用次数，是企业级 Agent 生产环境必备的治理组件。
     *
     * @param modelCallLimitHookDTO
     * @return
     */
    public ModelCallLimitHook creatModelCallLimitHook(ModelCallLimitHookDTO modelCallLimitHookDTO) {
        if (modelCallLimitHookDTO == null) {
            throw new IllegalArgumentException("humanInTheLoopHookDTO 不能为空");
        }

        ModelCallLimitHook.Builder builder = ModelCallLimitHook.builder();

        // 配置单次运行限制
        if (modelCallLimitHookDTO.getRunLimit() != null) {
            builder.runLimit(modelCallLimitHookDTO.getRunLimit());
        }

        // 配置会话级限制
        if (modelCallLimitHookDTO.getThreadLimit() != null) {
            builder.threadLimit(modelCallLimitHookDTO.getThreadLimit());
        }

        // 配置超限行为
        if (modelCallLimitHookDTO.getExitBehavior() != null) {
            builder.exitBehavior(modelCallLimitHookDTO.getExitBehavior());
        }

        return builder.build();
    }

    /**
     * 自动检测用户输入是否包含手机号、身份证、邮箱、银行卡等隐私信息，可选择拦截、脱敏、告警。
     *
     * @param dto
     * @return
     */
    public static PIIDetectionHook createPIIDetectionHook(PIIDetectionHookDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("PIIDetectionHookDTO 不能为空");
        }

        return PIIDetectionHook.builder()
                .piiType(dto.getPiiType())
                .strategy(dto.getStrategy())
                .detector(dto.getDetector())
                .applyToInput(dto.isApplyToInput())
                .applyToOutput(dto.isApplyToOutput())
                .applyToToolResults(dto.isApplyToToolResults())
                .build();
    }


}
