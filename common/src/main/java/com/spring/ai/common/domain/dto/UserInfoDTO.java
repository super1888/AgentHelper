package com.spring.ai.common.domain.dto;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/3/29
 */

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoDTO {


    private String name;
    private String emailAddress;
    private String phone;
    private String address;

}




