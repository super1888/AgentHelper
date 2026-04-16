package com.spring.ai.prompt.domain.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PromptTemplateStatisticsResponse {

    Integer totalCount;

    Integer enabledCount;

    Integer disabledCount;

    Integer inlineCount;

    Integer fileCount;
}
