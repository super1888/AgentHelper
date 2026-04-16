package com.spring.ai.user.domain.vo;

import lombok.Data;

/**
 * 租户详情展示对象。
 */
@Data
public class TenantProfileVO {

    private Long id;

    private String tenantCode;

    private String tenantName;

    private Integer status;

    private Integer isDefault;

    private Long ownerUserId;

    private String ownerUserName;

    private String contactName;

    private String contactPhone;

    private String description;

    private Long memberCount;
}
