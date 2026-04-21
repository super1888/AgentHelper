package com.spring.ai.tools.domain.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件用途：工具管理统计响应对象
 */
@Data
@Builder
public class ToolStatisticsResponse {

    private Integer totalCount;

    private Integer enabledCount;

    private Integer publishedCount;

    private Integer builtinCount;

    private Integer externalCount;

    private Integer highRiskCount;

    private Integer totalLogCount;

    private Integer successLogCount;

    private Integer failureLogCount;
}
