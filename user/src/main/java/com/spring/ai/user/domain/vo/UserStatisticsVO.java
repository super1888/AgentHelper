package com.spring.ai.user.domain.vo;

import lombok.Data;

@Data
public class UserStatisticsVO {

    /**
     * 总数
     */
    private long totalCount;

    /**
     * 启用数量
     */
    private long enabledCount;

    /**
     * 禁用数量
     */
    private long disabledCount;

    /**
     * 租户数量
     */
    private long tenantCount;
}
