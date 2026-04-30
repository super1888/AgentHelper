package com.spring.ai.agent.application.service.custom;

import com.spring.ai.agent.domain.dto.AuditResultDTO;
import com.spring.ai.agent.domain.dto.EnhancementResultDTO;
import com.spring.ai.agent.domain.dto.GenerationResultDTO;
import com.spring.ai.common.utils.CommonTextUtils;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 文档专家审核服务。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Component
public class DocumentExpertAuditService {

    @Resource
    private DocumentExpertLlmInvokeService documentExpertLlmInvokeService;

    @Resource
    private DocumentExpertModelSupportService documentExpertModelSupportService;

    /**
     * 执行双文档审核。
     */
    public AuditResultDTO audit(
            ChatClient chatClient,
            String userPrompt,
            EnhancementResultDTO enhancementResult,
            GenerationResultDTO documentA,
            GenerationResultDTO documentB
    ) {
        String prompt = """
                你是文档审核 Agent，需要同时审核两份候选文档。
                审核维度：
                1. 需求匹配度
                2. 内容完整性
                3. 文案质量（语病、错别字、逻辑）
                4. 合规性（敏感、违规、风险表述）
                5. 格式规范性（标题层级、段落排版）

                请执行以下动作：
                - 标记问题
                - 轻微问题直接修正
                - 严重问题明确指出
                - 分别输出文档A修订版、文档B修订版

                只返回 JSON：
                {
                  "summary": "一句话总结",
                  "issues": ["问题1", "问题2"],
                  "hasSevereIssue": false,
                  "reviewedDocumentA": "修订后的文档A",
                  "reviewedDocumentB": "修订后的文档B"
                }

                用户需求：
                %s

                标准化需求：
                %s

                文档A：
                %s

                文档B：
                %s
                """.formatted(
                userPrompt,
                enhancementResult.getStructuredInstruction(),
                documentA.getDocument(),
                documentB.getDocument()
        );

        AuditResultDTO result = documentExpertModelSupportService.parseJsonOrNull(
                documentExpertLlmInvokeService.call(chatClient, prompt),
                AuditResultDTO.class
        );
        if (result == null) {
            return new AuditResultDTO(
                    "审核结果解析失败，已沿用原始生成内容继续融合。",
                    List.of("审核层返回结果非标准 JSON，已走降级流程。"),
                    false,
                    documentA.getDocument(),
                    documentB.getDocument()
            );
        }
        return new AuditResultDTO(
                CommonTextUtils.defaultText(result.getSummary(), "文档审核完成"),
                CommonTextUtils.emptyIfNull(result.getIssues()),
                Boolean.TRUE.equals(result.getHasSevereIssue()),
                CommonTextUtils.defaultText(result.getReviewedDocumentA(), documentA.getDocument()),
                CommonTextUtils.defaultText(result.getReviewedDocumentB(), documentB.getDocument())
        );
    }

    /**
     * 构建审核阶段展示内容。
     */
    public String buildAuditStageContent(AuditResultDTO auditResult) {
        return """
                审核总结：
                %s

                修订版A：
                %s

                修订版B：
                %s
                """.formatted(
                auditResult.getSummary(),
                auditResult.getReviewedDocumentA(),
                auditResult.getReviewedDocumentB()
        );
    }
}
