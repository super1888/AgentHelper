package com.spring.ai.user.application.manager;

import cn.dev33.satoken.stp.StpUtil;
import com.spring.ai.common.constants.UserAuthConstants;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.enums.user.UserStatusEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyUserService;
import com.spring.ai.user.application.assmbler.UserAssembler;
import com.spring.ai.user.domain.request.UserLoginRequest;
import com.spring.ai.user.domain.vo.UserAuthLoginVO;
import com.spring.ai.user.domain.vo.UserProfileVO;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 认证应用管理器。
 */
@Component
public class AuthApplicationManager {

    @Resource
    private SyUserService syUserService;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 登录结果
     */
    public UserAuthLoginVO login(UserLoginRequest request) {
        SyUser user = syUserService.getByUsername(normalize(request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    ErrorCodeEnum.USER_PASSWORD_MISMATCH,
                    HttpStatus.UNAUTHORIZED,
                    ErrorCodeEnum.USER_PASSWORD_MISMATCH.getMessage()
            );
        }

        if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new BusinessException(
                    ErrorCodeEnum.USER_DISABLED,
                    HttpStatus.FORBIDDEN,
                    ErrorCodeEnum.USER_DISABLED.getMessage()
            );
        }

        StpUtil.login(user.getId());
        StpUtil.getSession().set(UserAuthConstants.LOGIN_NAME, user.getUsername());

        return UserAuthLoginVO.builder()
                .user(UserAssembler.toUserProfileVO(user))
                .token(UserAssembler.toUserTokenVO(user.getId()))
                .build();
    }

    /**
     * 退出登录。
     */
    public void logout() {
        StpUtil.checkLogin();
        StpUtil.logout();
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前登录用户信息
     */
    public UserProfileVO currentUser() {
        StpUtil.checkLogin();
        SyUser user = syUserService.getDetailById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new BusinessException(
                    ErrorCodeEnum.NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "当前登录用户不存在"
            );
        }
        return UserAssembler.toUserProfileVO(user);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
