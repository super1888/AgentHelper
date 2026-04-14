package com.spring.ai.user.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spring.ai.common.persistence.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体。
 * 这里先设计 Agent 系统的基础用户表，后续再逐步扩展租户、角色、登录体系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_user")
public class UserEntity extends BaseEntity {

    /**
     * 登录账号，建议全局唯一。
     */
    @TableField("username")
    private String username;

    /**
     * 用户显示名称。
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 手机号。
     */
    @TableField("phone")
    private String phone;

    /**
     * 邮箱。
     */
    @TableField("email")
    private String email;

    /**
     * 密码摘要，后续建议存加盐哈希。
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 用户状态，1-启用，0-禁用。
     */
    @TableField("status")
    private Integer status;

    /**
     * 所属租户 ID，单体阶段可先为空，后续支持多租户时再启用。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 备注信息。
     */
    @TableField("remark")
    private String remark;
}
