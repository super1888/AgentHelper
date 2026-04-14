package com.spring.ai.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAuthLoginVO {

    private UserProfileVO user;
    private UserTokenVO token;
}
