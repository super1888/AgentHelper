package com.spring.ai.user.application.manager;

import com.spring.ai.common.enums.user.UserStatusEnum;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyUserService;
import com.spring.ai.user.domain.request.UserRegisterRequest;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class UserApplicationManager {

    private static final int USER_STATUS_ENABLED = 1;

    @Resource
    SyUserService syUserService;

    @Resource
    PasswordEncoder passwordEncoder;

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
        entity.setStatus(UserStatusEnum.ENABLE.getCode());
        entity.setTenantId(null);
        syUserService.save(entity);
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
