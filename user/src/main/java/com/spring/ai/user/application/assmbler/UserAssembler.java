package com.spring.ai.user.application.assmbler;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.user.domain.vo.UserProfileVO;
import com.spring.ai.user.domain.vo.UserTokenVO;
import org.springframework.util.StringUtils;

/**
 * user实体转化类
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/14
 */
public class UserAssembler {

    public static UserTokenVO buildToken(Long userId) {
        String tokenName = SaManager.getConfig().getTokenName();
        String tokenPrefix = SaManager.getConfig().getTokenPrefix();
        String tokenValue = StpUtil.getTokenValue();
        String authorizationValue = StringUtils.hasText(tokenPrefix) ? tokenPrefix + " " + tokenValue : tokenValue;

        return UserTokenVO.builder()
                .tokenName(tokenName)
                .tokenPrefix(tokenPrefix)
                .tokenValue(tokenValue)
                .authorizationValue(authorizationValue)
                .expiresIn(StpUtil.getTokenTimeout())
                .loginId(userId)
                .build();
    }

    public static UserProfileVO toUserProfile(SyUser user) {
        return UserProfileVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus())
                .tenantId(user.getTenantId())
                .build();
    }

}
