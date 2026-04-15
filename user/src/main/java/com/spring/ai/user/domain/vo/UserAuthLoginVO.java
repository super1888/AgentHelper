package com.spring.ai.user.domain.vo;

import lombok.Data;

@Data
public class UserAuthLoginVO {

    private UserProfileVO user;
    private UserTokenVO token;
}
