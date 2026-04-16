package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户实体。
 *
 * <p>租户代表系统中的组织边界，用于隔离用户、Agent、会话等业务数据。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sy_tenant")
public class SyTenant extends BaseEntity {

    /**
     * 租户业务编码。
     */
    @TableField("tenant_code")
    private String tenantCode;

    /**
     * 租户名称。
     */
    @TableField("tenant_name")
    private String tenantName;

    /**
     * 租户状态，1 表示启用，0 表示禁用。
     */
    @TableField("status")
    private Integer status;

    /**
     * 是否为默认租户，1 表示默认租户。
     */
    @TableField("is_default")
    private Integer isDefault;

    /**
     * 默认租户归属用户 ID。
     */
    @TableField("owner_user_id")
    private Long ownerUserId;

    /**
     * 默认租户归属用户名。
     */
    @TableField("owner_user_name")
    private String ownerUserName;

    /**
     * 联系人姓名。
     */
    @TableField("contact_name")
    private String contactName;

    /**
     * 联系电话。
     */
    @TableField("contact_phone")
    private String contactPhone;

    /**
     * 租户描述。
     */
    @TableField("description")
    private String description;
}
