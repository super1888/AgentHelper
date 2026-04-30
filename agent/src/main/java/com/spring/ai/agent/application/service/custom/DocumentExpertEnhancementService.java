package com.spring.ai.agent.application.service.custom;

import com.spring.ai.agent.domain.dto.EnhancementResultDTO;
import com.spring.ai.common.utils.CommonTextUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 文档专家提示词增强服务。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Component
public class DocumentExpertEnhancementService {

    @Resource
    private DocumentExpertLlmInvokeService documentExpertLlmInvokeService;

    @Resource
    private DocumentExpertModelSupportService documentExpertModelSupportService;

    /**
     * 执行提示词增强。
     */
    public EnhancementResultDTO enhance(ChatClient chatClient, String userPrompt, boolean autoFillMissingInfo) {
        String prompt = """
                你是文档专家 Agent 的提示词增强器，需要把用户原始需求转换为标准化文档生成指令。
                需要提炼并补全以下字段：
                - documentType：文档类型
                - scenario：使用场景
                - audience：受众对象
                - tone：正式/专业/通俗/汇报型等
                - structureOutline：建议章节结构，数组形式
                - wordCountRequirement：字数要求
                - styleRequirement：行文风格要求
                - keyPoints：必须覆盖的核心要点，数组形式
                - needClarification：是否必须补充关键信息
                - clarificationQuestion：如果必须补充，请给出一个明确追问
                - missingItems：缺失项列表
                - summary：一句话说明增强结果
                - structuredInstruction：输出给后续生成 Agent 的完整标准指令

                规则：
                1. 如果用户需求可以按通用规则补全，请直接补全，不要轻易要求补问。
                2. 只有在缺失信息会直接影响文档可用性时，才将 needClarification 设为 true。
                3. 如果允许自动补全，请优先使用企业通用默认值。
                4. 仅返回 JSON，不要输出代码块，不要输出额外说明。

                autoFillMissingInfo=%s
                用户原始需求：
                %s
                """.formatted(autoFillMissingInfo, userPrompt);

        EnhancementResultDTO result = documentExpertModelSupportService.parseJsonOrNull(
                documentExpertLlmInvokeService.call(chatClient, prompt),
                EnhancementResultDTO.class
        );
        if (result == null) {
            return fallbackEnhancement(userPrompt, autoFillMissingInfo);
        }
        return new EnhancementResultDTO(
                CommonTextUtils.defaultText(result.getDocumentType(), "通用文档"),
                CommonTextUtils.defaultText(result.getScenario(), "通用业务沟通与交付场景"),
                CommonTextUtils.defaultText(result.getAudience(), "业务相关方"),
                CommonTextUtils.defaultText(result.getTone(), "专业正式"),
                CommonTextUtils.emptyIfNull(result.getStructureOutline()),
                CommonTextUtils.defaultText(result.getWordCountRequirement(), "1000-2000字"),
                CommonTextUtils.defaultText(result.getStyleRequirement(), "结构清晰、观点明确、便于直接交付"),
                CommonTextUtils.emptyIfNull(result.getKeyPoints()),
                Boolean.TRUE.equals(result.getNeedClarification()),
                result.getClarificationQuestion(),
                CommonTextUtils.emptyIfNull(result.getMissingItems()),
                CommonTextUtils.defaultText(result.getSummary(), "已完成文档需求增强"),
                CommonTextUtils.defaultText(result.getStructuredInstruction(), userPrompt)
        );
    }

    /**
     * 缺少结构化增强结果时的兜底处理。
     */
    public EnhancementResultDTO fallbackEnhancement(String userPrompt, boolean autoFillMissingInfo) {
        List<String> missingItems = new ArrayList<>();
        missingItems.add("文档类型");
        missingItems.add("使用场景");
        missingItems.add("目标受众");
        return new EnhancementResultDTO(
                "通用业务文档",
                "通用业务沟通与交付场景",
                "业务相关方",
                "专业正式",
                List.of("一、背景与目标", "二、主要内容", "三、执行安排", "四、风险与保障", "五、总结"),
                "1000-2000字",
                "结构完整、表达清晰、便于直接交付",
                List.of("覆盖用户明确提出的核心诉求", "补全必要章节", "确保逻辑完整"),
                !autoFillMissingInfo,
                autoFillMissingInfo ? null : "请补充文档类型、使用场景和目标受众。",
                autoFillMissingInfo ? List.of() : missingItems,
                "已按默认规则完成文档需求补全。",
                """
                        请根据以下要求生成一份完整文档：
                        - 原始需求：%s
                        - 文档类型：通用业务文档
                        - 使用场景：通用业务沟通与交付场景
                        - 目标受众：业务相关方
                        - 行文风格：专业正式
                        - 建议结构：一、背景与目标；二、主要内容；三、执行安排；四、风险与保障；五、总结
                        - 输出要求：结构清晰、内容完整、可直接交付
                        """.formatted(userPrompt)
        );
    }
}
