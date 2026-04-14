package com.spring.ai.user.application.manager;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.constants.UserAuthConstants;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyUserService;
import com.spring.ai.user.application.assmbler.AuthAssembler;
import com.spring.ai.user.domain.request.UserLoginRequest;
import com.spring.ai.user.domain.vo.UserAuthLoginVO;
import com.spring.ai.user.domain.vo.UserProfileVO;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthApplicationManager {

    private static final int USER_STATUS_ENABLED = 1;

    @Resource
    SyUserService syUserService;

    @Resource
    PasswordEncoder passwordEncoder;

    public UserAuthLoginVO login(UserLoginRequest request) {
        String username = normalize(request.getUsername());
        SyUser user = syUserService.getOne(
                new LambdaQueryWrapper<SyUser>()
                        .eq(SyUser::getUsername, username)
                        .last(SqlConstants.LIMIT_ONE)
        );

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    ErrorCodeEnum.USER_PASSWORD_MISMATCH,
                    HttpStatus.UNAUTHORIZED,
                    ErrorCodeEnum.USER_PASSWORD_MISMATCH.getMessage()
            );
        }

        if (user.getStatus() != null && user.getStatus() != USER_STATUS_ENABLED) {
            throw new BusinessException(
                    ErrorCodeEnum.USER_DISABLED,
                    HttpStatus.FORBIDDEN,
                    ErrorCodeEnum.USER_DISABLED.getMessage()
            );
        }

        StpUtil.login(user.getId());
        StpUtil.getSession().set(UserAuthConstants.LOGIN_NAME, user.getUsername());
        return UserAuthLoginVO.builder()
                .user(AuthAssembler.toUserProfile(user))
                .token(AuthAssembler.buildToken(user.getId()))
                .build();
    }

    public void logout() {
        StpUtil.checkLogin();
        StpUtil.logout();
    }

    public UserProfileVO currentUser() {
        StpUtil.checkLogin();
        SyUser user = syUserService.getById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException(
                    ErrorCodeEnum.NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "当前登录用户不存在"
            );
        }
        return AuthAssembler.toUserProfile(user);
    }


    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return StringUtils.hasText(normalized) ? normalized : null;
    }
}
