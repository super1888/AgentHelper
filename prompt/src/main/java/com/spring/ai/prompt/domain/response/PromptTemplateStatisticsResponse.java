package com.spring.ai.prompt.domain.response;

import lombok.Builder;
import lombok.Value;

/**
 * 文件用途：封装提示词模板统计结果。
 * 核心功能：返回模板总量、启停分布和来源分布。
 */
@Value
@Builder
public class PromptTemplateStatisticsResponse {

    Integer totalCount;

    Integer enabledCount;

    Integer disabledCount;

    Integer inlineCount;

    Integer fileCount;
}
