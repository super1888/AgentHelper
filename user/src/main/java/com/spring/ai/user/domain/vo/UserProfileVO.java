package com.spring.ai.user.domain.vo;

import lombok.Data;

/**
 * 用户资料展示对象。
 */
@Data
public class UserProfileVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String email;

    private Integer status;

    private Long tenantId;

    private String tenantName;
}
