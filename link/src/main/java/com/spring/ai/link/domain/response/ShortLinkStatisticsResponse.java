package com.spring.ai.link.domain.response;

import lombok.Data;

/**
 * 短链接整体统计响应。
 */
@Data
public class ShortLinkStatisticsResponse {

    private Long totalCount;

    private Long enabledCount;

    private Long expiredCount;

    private Long totalVisitCount;

    private Long uniqueVisitorCount;

    private Long uniqueIpCount;
}
