package com.spring.ai.hooks.application.manager;

import com.spring.ai.hooks.domain.dto.HookCatalogDTO;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 文件用途：Hook 目录注册表
 * 核心职责：提供常见 Agent Hook 的商业化模板
 */
@Component
public class HookCatalogRegistry {

    private final List<HookCatalogDTO> catalog = new ArrayList<>();

    @PostConstruct
    public void init() {
        catalog.add(HookCatalogDTO.builder()
                .hookKey("rag_context_guard")
                .hookName("RAG 上下文守护")
                .description("在模型调用前执行召回结果压缩、噪声过滤和敏感片段屏蔽。")
                .hookType("MESSAGES_MODEL")
                .hookStage("PRE_MODEL")
                .riskLevel("MEDIUM")
                .failStrategy("CONTINUE")
                .defaultConfigJson("{\"topK\":8,\"maxContextTokens\":3000,\"sensitivePatternEnabled\":true}")
                .defaultTestPayloadJson("{\"question\":\"请总结退款规则\",\"documents\":[\"规则A\",\"规则B\"]}")
                .tags(List.of("RAG", "CONTEXT", "GUARD"))
                .build());
        catalog.add(HookCatalogDTO.builder()
                .hookKey("tool_call_risk_control")
                .hookName("工具调用风控")
                .description("在工具调用前执行参数校验、风险评分和审批门禁。")
                .hookType("AGENT")
                .hookStage("PRE_TOOL_CALL")
                .riskLevel("HIGH")
                .failStrategy("BLOCK")
                .defaultConfigJson("{\"approvalRequiredRiskLevel\":\"HIGH\",\"blockedTools\":[\"DELETE_ORDER\",\"EXPORT_PII\"],\"maxAmount\":10000}")
                .defaultTestPayloadJson("{\"toolCode\":\"EXPORT_PII\",\"arguments\":{\"tenantId\":1}}")
                .tags(List.of("TOOL", "RISK", "APPROVAL"))
                .build());
        catalog.add(HookCatalogDTO.builder()
                .hookKey("response_compliance_check")
                .hookName("响应合规校验")
                .description("在输出前对敏感词、承诺性表述和格式规范进行治理。")
                .hookType("MODEL")
                .hookStage("POST_MODEL")
                .riskLevel("MEDIUM")
                .failStrategy("FALLBACK")
                .defaultConfigJson("{\"piiMaskEnabled\":true,\"forbiddenClaims\":[\"绝对保证\",\"100%成功\"],\"rewriteOnViolation\":true}")
                .defaultTestPayloadJson("{\"answer\":\"我保证这笔贷款 100% 会通过\"}")
                .tags(List.of("COMPLIANCE", "OUTPUT", "FILTER"))
                .build());
    }

    /**
     * 查询内置 Hook 目录。
     */
    public List<HookCatalogDTO> listCatalog() {
        return List.copyOf(catalog);
    }
}
