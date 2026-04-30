package com.spring.ai.agent.application.assmbler;

import com.spring.ai.agent.domain.response.DocumentExpertChatResponse.StageResult;
import java.util.List;

/**
 * 自定义 Agent 组装器。
 * 负责自定义 Agent 响应阶段结果的统一组装。
 *
 * @author zhouqi
 * @since 2026/4/30
 */
public final class CustomAgentAssembler {

    private CustomAgentAssembler() {
    }

    /**
     * 构建阶段结果。
     *
     * @param stageName 阶段名称
     * @param status 阶段状态
     * @param modelCode 模型编码
     * @param summary 阶段摘要
     * @param content 阶段内容
     * @param issues 阶段问题
     * @return 阶段结果
     */
    public static StageResult buildStageResult(
            String stageName,
            String status,
            String modelCode,
            String summary,
            String content,
            List<String> issues
    ) {
        return StageResult.builder()
                .stageName(stageName)
                .status(status)
                .modelCode(modelCode)
                .summary(summary)
                .content(content)
                .issues(issues)
                .build();
    }
}
