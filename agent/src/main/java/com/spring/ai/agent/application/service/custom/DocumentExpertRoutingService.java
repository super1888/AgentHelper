package com.spring.ai.agent.application.service.custom;

import com.spring.ai.agent.domain.dto.AccessDecisionDTO;
import com.spring.ai.common.utils.CommonTextUtils;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * 文档专家路由与准入服务。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
@Component
public class DocumentExpertRoutingService {

    private static final List<String> DOCUMENT_RELATED_KEYWORDS = List.of(
            "文档", "方案", "报告", "制度", "手册", "汇报", "纪要", "总结", "规划", "说明书", "说明文",
            "流程", "规范", "sop", "操作手册", "实施方案", "可研", "需求文档", "设计文档", "培训材料"
    );

    private static final List<String> NON_DOCUMENT_BLOCK_KEYWORDS = List.of(
            "天气", "股票", "旅游", "吃什么", "恋爱", "游戏攻略", "闲聊", "算命", "写代码", "修bug", "报错"
    );

    @Resource
    private DocumentExpertLlmInvokeService documentExpertLlmInvokeService;

    @Resource
    private DocumentExpertModelSupportService documentExpertModelSupportService;

    /**
     * 执行路由与准入判断。
     */
    public AccessDecisionDTO route(ChatClient chatClient, String userPrompt) {
        if (CommonTextUtils.containsAnyKeyword(userPrompt, NON_DOCUMENT_BLOCK_KEYWORDS)
                && !CommonTextUtils.containsAnyKeyword(userPrompt, DOCUMENT_RELATED_KEYWORDS)) {
            return new AccessDecisionDTO(false, "当前请求不属于文档类需求，文档专家 Agent 已拦截。", null,
                    List.of("命中明显非文档类关键词"));
        }

        String prompt = """
                你是文档专家 Agent 的领域准入校验器，只负责判断请求是否属于文档相关需求。
                可通过的需求范围：文档编写、文档优化、文档整理、方案、报告、制度、手册、纪要、汇报、说明书、规范、SOP、培训材料、需求文档、设计文档。
                不可通过的需求范围：纯闲聊、生活问答、代码调试、通用技术问答、娱乐、情感、旅游、天气、股票等。

                请仅返回 JSON，不要输出额外解释：
                {
                  "allowed": true,
                  "reason": "一句话说明原因",
                  "normalizedIntent": "用一句话归纳用户文档意图",
                  "issues": ["若有问题可列出，否则返回空数组"]
                }

                用户请求：
                %s
                """.formatted(userPrompt);

        AccessDecisionDTO decision = documentExpertModelSupportService.parseJsonOrNull(
                documentExpertLlmInvokeService.call(chatClient, prompt, "路由与准入校验", "当前阶段模型"),
                AccessDecisionDTO.class
        );
        if (decision == null) {
            return new AccessDecisionDTO(true, "准入校验完成，按文档类需求继续处理。", userPrompt, List.of());
        }
        return new AccessDecisionDTO(
                Boolean.TRUE.equals(decision.getAllowed()),
                CommonTextUtils.defaultText(decision.getReason(), "准入校验完成"),
                CommonTextUtils.defaultText(decision.getNormalizedIntent(), userPrompt),
                CommonTextUtils.emptyIfNull(decision.getIssues())
        );
    }
}
