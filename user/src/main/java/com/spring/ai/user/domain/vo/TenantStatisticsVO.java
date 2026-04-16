package com.spring.ai.user.domain.vo;

import lombok.Data;

/**
 * 租户统计展示对象。
 */
@Data
public class TenantStatisticsVO {

    private Long totalCount;

    private Long enabledCount;

    private Long disabledCount;
}
