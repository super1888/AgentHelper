package com.spring.ai.user.application.provider;

import cn.dev33.satoken.stp.StpUtil;
import com.spring.ai.common.constants.UserAuthConstants;
import com.spring.ai.common.providerInterface.UserProvider;
import org.springframework.stereotype.Component;

/**
 * class information
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/14
 */
@Component
public class SaTokenUserProvider implements UserProvider {

    @Override
    public Long getCurrentUserId() {
        if (StpUtil.isLogin()) {
            return StpUtil.getLoginIdAsLong();
        }
        return null;
    }

    @Override
    public String getCurrentUserName() {
        if (StpUtil.isLogin()) {
            return (String) StpUtil.getSession().get(UserAuthConstants.LOGIN_NAME);
        }
        return null;
    }
}
