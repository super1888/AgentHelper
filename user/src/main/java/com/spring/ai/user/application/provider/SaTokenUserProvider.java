package com.spring.ai.user.application.provider;

import cn.dev33.satoken.stp.StpUtil;
import com.spring.ai.common.constants.UserAuthConstants;
import com.spring.ai.common.providerInterface.UserProvider;
import org.springframework.stereotype.Component;

/**
 * 基于 Sa-Token 的当前登录用户提供器。
 */
@Component
public class SaTokenUserProvider implements UserProvider {

    @Override
    public Long getCurrentUserId() {
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginIdAsLong();
        }
        return 1L;
    }

    @Override
    public String getCurrentUserName() {
        if (StpUtil.isLogin()) {
            return (String) StpUtil.getSession().get(UserAuthConstants.LOGIN_NAME);
        }
        return "admin";
    }
}
