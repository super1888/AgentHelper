package com.spring.ai.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserTokenVO {

    private String tokenName;
    private String tokenPrefix;
    private String tokenValue;
    private String authorizationValue;
    private long expiresIn;
    private Long loginId;
}
