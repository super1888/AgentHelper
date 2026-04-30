package com.spring.ai.agent.application.service.custom;

import com.spring.ai.agent.domain.dto.EnhancementResultDTO;
import com.spring.ai.agent.domain.dto.GenerationResultDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 文档专家生成服务。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Component
public class DocumentExpertGenerationService {

    @Resource
    private DocumentExpertLlmInvokeService documentExpertLlmInvokeService;

    /**
     * 生成正式结构化文档。
     */
    public GenerationResultDTO generateStructured(String modelCode, EnhancementResultDTO enhancementResult) {
        String prompt = """
                你是文档生成 Agent A，定位为“官方结构化文档写作专家”。
                目标：
                1. 输出正式、规范、结构化的完整文档
                2. 适合方案、制度、报告、汇报、规范类场景
                3. 使用标准章节标题，层次清楚，语言严谨

                生成要求：
                - 优先保证结构完整、条理清晰
                - 如用户未指定格式，采用标准公文/方案式章节
                - 输出最终文档正文，不要解释

                标准化需求：
                %s
                """.formatted(enhancementResult.getStructuredInstruction());
        return new GenerationResultDTO(
                "已生成正式结构化版本文档。",
                documentExpertLlmInvokeService.callByModelCode(modelCode, prompt, "双文档生成-A")
        );
    }

    /**
     * 生成通俗精简文档。
     */
    public GenerationResultDTO generateReadable(String modelCode, EnhancementResultDTO enhancementResult) {
        String prompt = """
                你是文档生成 Agent B，定位为“通俗精简文档写作专家”。
                目标：
                1. 输出逻辑清晰、语言通俗、便于阅读和执行的完整文档
                2. 适合操作手册、说明文档、纪要、宣导材料
                3. 在保证完整性的前提下提升易懂性和落地性

                生成要求：
                - 保留核心结构，但表达尽量直白
                - 增强可执行步骤、要点清单、注意事项
                - 输出最终文档正文，不要解释

                标准化需求：
                %s
                """.formatted(enhancementResult.getStructuredInstruction());
        return new GenerationResultDTO(
                "已生成通俗精简版本文档。",
                documentExpertLlmInvokeService.callByModelCode(modelCode, prompt, "双文档生成-B")
        );
    }
}
