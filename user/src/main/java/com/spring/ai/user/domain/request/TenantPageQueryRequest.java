package com.spring.ai.user.domain.request;

import lombok.Data;

/**
 * 租户分页查询请求。
 */
@Data
public class TenantPageQueryRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private String tenantCode;

    private String tenantName;

    private Integer status;
}
