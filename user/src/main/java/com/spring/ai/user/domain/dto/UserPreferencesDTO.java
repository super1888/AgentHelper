package com.spring.ai.user.domain.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户偏好设置
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/7
 */
@Data
@Builder
public class UserPreferencesDTO {

    private String communicationStyle;

    private String language;

    private String interests;

    public UserPreferencesDTO getPreferences(String userId) {
        return UserPreferencesDTO.builder().build();
    }
}
