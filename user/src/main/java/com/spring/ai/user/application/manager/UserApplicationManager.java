package com.spring.ai.user.application.manager;

import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.enums.user.UserStatusEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyUserService;
import com.spring.ai.user.application.assmbler.UserAssembler;
import com.spring.ai.user.domain.request.UserCreateRequest;
import com.spring.ai.user.domain.request.UserRegisterRequest;
import com.spring.ai.user.domain.request.UserUpdateRequest;
import com.spring.ai.user.domain.vo.UserProfileVO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户管理应用管理器。
 */
@Component
public class UserApplicationManager {

    @Resource
    private SyUserService syUserService;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 用户注册。
     *
     * @param request 注册请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterRequest request) {
        UserCreateRequest createRequest = new UserCreateRequest();
        createRequest.setUsername(request.getUsername());
        createRequest.setNickname(request.getNickname());
        createRequest.setPhone(request.getPhone());
        createRequest.setEmail(request.getEmail());
        createRequest.setPassword(request.getPassword());
        createRequest.setConfirmPassword(request.getConfirmPassword());
        createRequest.setStatus(UserStatusEnum.ENABLE.getCode());
        createRequest.setTenantId(null);
        createUser(createRequest);
    }

    /**
     * 新增用户。
     *
     * @param request 新增请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserCreateRequest request) {
        UserStatusEnum.fromVal(request.getStatus());
        validatePassword(request.getPassword(), request.getConfirmPassword());
        validateUnique(null, request.getUsername(), request.getPhone(), request.getEmail());

        SyUser user = UserAssembler.toCreateEntity(request, passwordEncoder.encode(request.getPassword()));
        syUserService.save(user);
    }

    /**
     * 删除用户。
     *
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        SyUser user = requireUser(userId);
        if (!syUserService.deleteByUserId(user.getId())) {
            throw new BusinessException(
                    ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "删除用户失败"
            );
        }
    }

    /**
     * 编辑用户信息。
     *
     * @param userId  用户 ID
     * @param request 编辑请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UserUpdateRequest request) {
        UserStatusEnum.fromVal(request.getStatus());
        SyUser user = requireUser(userId);
        validateUnique(userId, user.getUsername(), request.getPhone(), request.getEmail());
        UserAssembler.mergeForUpdate(user, request);
        syUserService.updateById(user);
    }

    /**
     * 查询用户信息明细。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    public UserProfileVO getUserDetail(Long userId) {
        return UserAssembler.toUserProfileVO(requireUser(userId));
    }

    /**
     * 查询所有用户。
     *
     * @return 用户列表
     */
    public List<UserProfileVO> listAllUsers() {
        return UserAssembler.toUserProfileVOList(syUserService.listAllUsers());
    }

    private SyUser requireUser(Long userId) {
        SyUser user = syUserService.getDetailById(userId);
        if (user == null) {
            throw new BusinessException(
                    ErrorCodeEnum.NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "用户不存在"
            );
        }
        return user;
    }

    private void validatePassword(String password, String confirmPassword) {
        if (!StringUtils.hasText(password) || !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
    }

    private void validateUnique(Long currentUserId, String username, String phone, String email) {
        SyUser usernameUser = syUserService.getByUsername(normalize(username));
        if (usernameUser != null && !sameUser(currentUserId, usernameUser.getId())) {
            throw new BusinessException(
                    ErrorCodeEnum.USER_ALREADY_EXISTS,
                    HttpStatus.CONFLICT,
                    "用户名已存在"
            );
        }

        if (StringUtils.hasText(phone)) {
            SyUser phoneUser = syUserService.getByPhone(normalize(phone));
            if (phoneUser != null && !sameUser(currentUserId, phoneUser.getId())) {
                throw new BusinessException(
                        ErrorCodeEnum.USER_ALREADY_EXISTS,
                        HttpStatus.CONFLICT,
                        "手机号已存在"
                );
            }
        }

        if (StringUtils.hasText(email)) {
            SyUser emailUser = syUserService.getByEmail(normalize(email));
            if (emailUser != null && !sameUser(currentUserId, emailUser.getId())) {
                throw new BusinessException(
                        ErrorCodeEnum.USER_ALREADY_EXISTS,
                        HttpStatus.CONFLICT,
                        "邮箱已存在"
                );
            }
        }
    }

    private boolean sameUser(Long currentUserId, Long targetUserId) {
        return currentUserId != null && currentUserId.equals(targetUserId);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
