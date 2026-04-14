package com.spring.ai.common.repository.enitiy;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.spring.ai.common.domain.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author auto-generator
 * @since 2026-04-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sy_user")
public class SyUser extends BaseEntity {


    /**
     * 登录账号
     */
    @TableField("username")
    private String username;

    /**
     * 用户显示名称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 密码摘要
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 用户状态: 1-启用, 0-禁用
     */
    @TableField("status")
    private Integer status;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;


}
