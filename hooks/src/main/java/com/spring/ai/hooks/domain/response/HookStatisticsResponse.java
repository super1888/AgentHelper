package com.spring.ai.hooks.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：Hook 统计响应
 */
@Data
@Builder
public class HookStatisticsResponse {

    private Integer totalCount;
    private Integer enabledCount;
    private Integer publishedCount;
    private Integer hotUpdateEnabledCount;
    private Integer deletedCount;
    private Integer highRiskCount;
    private Integer totalBindingCount;
    private Integer totalTestCaseCount;
    private Integer totalLogCount;
    private Integer successLogCount;
    private Integer failureLogCount;
}
