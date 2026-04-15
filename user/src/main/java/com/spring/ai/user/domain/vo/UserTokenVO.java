package com.spring.ai.user.domain.vo;

import lombok.Data;

/**
 * 用户令值对象(VO)
 * 用于封装用户登录后生成的令牌相关信息
 */
@Data
public class UserTokenVO {

    private String tokenName;
    private String tokenPrefix;
    private String tokenValue;
    private String authorizationValue;
    private long expiresIn;
    private Long loginId;
}
