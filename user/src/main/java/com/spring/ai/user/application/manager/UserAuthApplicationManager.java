package com.spring.ai.user.application.manager;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyUserService;
import com.spring.ai.user.application.assmbler.UserAssembler;
import com.spring.ai.user.domain.dto.UserLoginRequest;
import com.spring.ai.user.domain.dto.UserRegisterRequest;
import com.spring.ai.user.domain.vo.UserAuthLoginVO;
import com.spring.ai.user.domain.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserAuthApplicationManager {

    private static final int USER_STATUS_ENABLED = 1;

    private final SyUserService syUserService;

    private final PasswordEncoder passwordEncoder;

    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterRequest request) {
        validateRegisterRequest(request);

        String username = normalize(request.getUsername());
        String phone = normalizeNullable(request.getPhone());
        String email = normalizeNullable(request.getEmail());

        SyUser entity = new SyUser();
        entity.setUsername(username);
        entity.setNickname(resolveNickname(request));
        entity.setPhone(phone);
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        entity.setStatus(USER_STATUS_ENABLED);
        entity.setTenantId(null);
        syUserService.save(entity);
    }

    public UserAuthLoginVO login(UserLoginRequest request) {
        String username = normalize(request.getUsername());
        SyUser user = syUserService.getOne(
                new LambdaQueryWrapper<SyUser>()
                        .eq(SyUser::getUsername, username)
                        .last("limit 1")
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

        return UserAuthLoginVO.builder()
                .user(UserAssembler.toUserProfile(user))
                .token(UserAssembler.buildToken(user.getId()))
                .build();
    }

    public void logout() {
        StpUtil.checkLogin();
        StpUtil.logout();
    }

    public UserProfileVO currentUser() {
        StpUtil.checkLogin();
        SyUser user = syUserService.getById(Long.valueOf(String.valueOf(StpUtil.getLoginId())));
        if (user == null) {
            throw new BusinessException(
                    ErrorCodeEnum.NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "当前登录用户不存在"
            );
        }
        return UserAssembler.toUserProfile(user);
    }

    private void validateRegisterRequest(UserRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
    }




    private String resolveNickname(UserRegisterRequest request) {
        String nickname = normalizeNullable(request.getNickname());
        return StringUtils.hasText(nickname) ? nickname : normalize(request.getUsername());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return StringUtils.hasText(normalized) ? normalized : null;
    }
}
