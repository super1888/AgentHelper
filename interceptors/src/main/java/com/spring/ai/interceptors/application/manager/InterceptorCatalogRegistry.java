package com.spring.ai.interceptors.application.manager;

import com.spring.ai.common.enums.InterceptorTypeEnum;
import com.spring.ai.interceptors.config.InterceptorManagementConstants;
import com.spring.ai.interceptors.domain.dto.InterceptorCatalogDTO;
import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 文件用途：Interceptor 内置目录注册器
 * 核心职责：维护系统内置拦截器模板的目录信息与默认配置
 */
@Component
public class InterceptorCatalogRegistry {

    @Resource
    private InterceptorSupportManager interceptorSupportManager;

    /**
     * 查询内置拦截器目录。
     */
    public List<InterceptorCatalogDTO> listCatalog() {
        return List.of(
                buildCatalog(
                        InterceptorTypeEnum.TOOL_RETRY.name(),
                        "工具重试拦截器",
                        "在工具调用失败、超时或异常时执行重试和退避策略。",
                        "TOOL",
                        InterceptorManagementConstants.STAGE_POST_TOOL,
                        InterceptorManagementConstants.RISK_LEVEL_MEDIUM,
                        "CONTINUE",
                        Map.of(
                                "maxRetries", 2,
                                "toolNames", List.of("web_search", "ticket_query"),
                                "backoffFactor", 2.0,
                                "initialDelayMs", 200,
                                "maxDelayMs", 3000,
                                "jitter", true
                        ),
                        Map.of(
                                "toolName", "web_search",
                                "toolStatus", "FAILED",
                                "errorMessage", "gateway timeout"
                        ),
                        List.of("stability", "tooling", "retry")
                ),
                buildCatalog(
                        InterceptorTypeEnum.TODO_LIST.name(),
                        "待办拆解拦截器",
                        "将复杂任务切分为可执行待办，适合长流程 Agent 编排。",
                        "AGENT",
                        InterceptorManagementConstants.STAGE_PRE_MODEL,
                        InterceptorManagementConstants.RISK_LEVEL_LOW,
                        "CONTINUE",
                        Map.of(
                                "systemPrompt", "你负责把复杂用户需求拆成待办清单。",
                                "toolDescription", "负责生成结构化待办项"
                        ),
                        Map.of(
                                "input", "请帮我完成客户续约、风险评估和合同发送"
                        ),
                        List.of("planning", "task-breakdown")
                ),
                buildCatalog(
                        InterceptorTypeEnum.TOOL_SELECTION.name(),
                        "工具选择拦截器",
                        "在多工具场景下收敛工具范围，避免工具泛化调用。",
                        "TOOL",
                        InterceptorManagementConstants.STAGE_PRE_TOOL,
                        InterceptorManagementConstants.RISK_LEVEL_HIGH,
                        "BLOCK",
                        Map.of(
                                "systemPrompt", "根据任务目标筛选最少必要工具。",
                                "maxTools", 2,
                                "alwaysInclude", List.of("audit_logger")
                        ),
                        Map.of(
                                "input", "请查询订单并给出退款方案",
                                "toolCandidates", List.of("order_query", "refund_calculator", "crm_search")
                        ),
                        List.of("routing", "governance", "tooling")
                ),
                buildCatalog(
                        InterceptorTypeEnum.TOOL_EMULATOR.name(),
                        "工具模拟拦截器",
                        "在联调和预发阶段用模拟响应替代真实工具，降低外部依赖风险。",
                        "TOOL",
                        InterceptorManagementConstants.STAGE_PRE_TOOL,
                        InterceptorManagementConstants.RISK_LEVEL_MEDIUM,
                        "CONTINUE",
                        Map.of(
                                "emulateAll", false,
                                "toolsToEmulate", List.of("ticket_query"),
                                "promptTemplate", "你是一个工具模拟器，请返回稳定 JSON。",
                                "mockResponses", Map.of("ticket_query", Map.of("ticketId", "T-10001", "status", "OPEN"))
                        ),
                        Map.of(
                                "toolCalls", List.of(Map.of("name", "ticket_query", "arguments", Map.of("ticketId", "T-10001")))
                        ),
                        List.of("sandbox", "testing", "tooling")
                ),
                buildCatalog(
                        InterceptorTypeEnum.CONTEXT_EDITING.name(),
                        "上下文裁剪拦截器",
                        "在长上下文会话中保留关键消息并清理高噪声内容。",
                        "MODEL",
                        InterceptorManagementConstants.STAGE_PRE_MODEL,
                        InterceptorManagementConstants.RISK_LEVEL_MEDIUM,
                        "CONTINUE",
                        Map.of(
                                "clearAtLeast", 4,
                                "keep", 6,
                                "clearToolInputs", true,
                                "placeholder", "[工具输入已裁剪]",
                                "excludeTools", List.of("audit_logger")
                        ),
                        Map.of(
                                "messages", List.of(
                                        Map.of("role", "user", "content", "第一轮上下文"),
                                        Map.of("role", "assistant", "content", "第二轮上下文"),
                                        Map.of("role", "tool", "content", "大型工具返回内容")
                                )
                        ),
                        List.of("context", "memory", "cost-control")
                )
        );
    }

    private InterceptorCatalogDTO buildCatalog(
            String interceptorKey,
            String interceptorName,
            String description,
            String interceptorType,
            String interceptorStage,
            String riskLevel,
            String failStrategy,
            Map<String, Object> defaultConfig,
            Map<String, Object> defaultTestPayload,
            List<String> tags
    ) {
        return InterceptorCatalogDTO.builder()
                .interceptorKey(interceptorKey)
                .interceptorName(interceptorName)
                .description(description)
                .interceptorType(interceptorType)
                .interceptorStage(interceptorStage)
                .riskLevel(riskLevel)
                .failStrategy(failStrategy)
                .defaultConfigJson(interceptorSupportManager.prettyJson(defaultConfig))
                .defaultTestPayloadJson(interceptorSupportManager.prettyJson(defaultTestPayload))
                .tags(tags)
                .build();
    }
}
