package com.spring.ai.user.domain.vo;

import lombok.Data;

/**
 * 租户选项展示对象。
 */
@Data
public class TenantOptionVO {

    private Long id;

    private String tenantCode;

    private String tenantName;

    private Integer status;
}
