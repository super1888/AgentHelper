package com.spring.ai.agent.application.service.custom;

import com.spring.ai.agent.domain.dto.AuditResultDTO;
import com.spring.ai.agent.domain.dto.EnhancementResultDTO;
import com.spring.ai.agent.domain.dto.FusionResultDTO;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 文档专家融合服务。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Component
public class DocumentExpertFusionService {

    @Resource
    private DocumentExpertLlmInvokeService documentExpertLlmInvokeService;

    /**
     * 执行文档融合。
     */
    public FusionResultDTO fuse(
            ChatClient chatClient,
            String userPrompt,
            EnhancementResultDTO enhancementResult,
            AuditResultDTO auditResult
    ) {
        String prompt = """
                你是双文档融合汇总 Agent。
                任务：
                1. 接收两份已审核文档
                2. 去重合并共性内容
                3. 保留 A 的正式结构
                4. 融合 B 的通俗表达和可执行性
                5. 统一标题层级、排版和行文风格
                6. 输出一份可直接交付的最终完整文档

                输出要求：
                - 直接输出最终成稿，不要解释
                - 不要提及“文档A/文档B”
                - 不要输出审核痕迹

                用户需求：
                %s

                标准化需求：
                %s

                审核后文档A：
                %s

                审核后文档B：
                %s
                """.formatted(
                userPrompt,
                enhancementResult.getStructuredInstruction(),
                auditResult.getReviewedDocumentA(),
                auditResult.getReviewedDocumentB()
        );
        return new FusionResultDTO(
                "已完成双文档融合并生成最终成稿。",
                documentExpertLlmInvokeService.call(chatClient, prompt)
        );
    }
}
