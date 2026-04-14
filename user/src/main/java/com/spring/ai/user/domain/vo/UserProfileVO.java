package com.spring.ai.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private Integer status;
    private Long tenantId;
}
