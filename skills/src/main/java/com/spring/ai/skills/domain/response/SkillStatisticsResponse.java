package com.spring.ai.skills.domain.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillStatisticsResponse {

    private Integer totalCount;

    private Integer enabledCount;

    private Integer publishedCount;

    private Integer hotUpdateEnabledCount;
}
