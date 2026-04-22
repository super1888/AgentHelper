package com.spring.ai.user.application.manager;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.enums.user.UserStatusEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.exception.BusinessExceptions;
import com.spring.ai.common.repository.enitiy.SyTenant;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyTenantService;
import com.spring.ai.common.repository.service.SyUserService;
import com.spring.ai.user.application.assmbler.UserAssembler;
import com.spring.ai.user.domain.request.UserCreateRequest;
import com.spring.ai.user.domain.request.UserPageQueryRequest;
import com.spring.ai.user.domain.request.UserRegisterRequest;
import com.spring.ai.user.domain.request.UserUpdateRequest;
import com.spring.ai.user.domain.vo.UserProfileVO;
import com.spring.ai.user.domain.vo.UserStatisticsVO;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
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
    private SyTenantService syTenantService;

    @Resource
    private TenantApplicationManager tenantApplicationManager;

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
        validateTenant(request.getTenantId());

        SyUser user = UserAssembler.toCreateEntity(request, passwordEncoder.encode(request.getPassword()));
        syUserService.save(user);
        if (user.getTenantId() == null) {
            tenantApplicationManager.initializeDefaultTenant(user);
        }
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
     * @param userId 用户 ID
     * @param request 编辑请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UserUpdateRequest request) {
        UserStatusEnum.fromVal(request.getStatus());
        validateTenant(request.getTenantId());

        SyUser user = requireUser(userId);
        validateUnique(userId, user.getUsername(), request.getPhone(), request.getEmail());
        UserAssembler.mergeForUpdate(user, request);
        syUserService.updateById(user);
        if (user.getTenantId() == null) {
            tenantApplicationManager.initializeDefaultTenant(user);
        }
    }

    /**
     * 查询用户信息详情。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    public UserProfileVO getUserDetail(Long userId) {
        SyUser user = requireUser(userId);
        return UserAssembler.toUserProfileVO(user, resolveTenantName(user.getTenantId()));
    }

    /**
     * 查询所有用户。
     *
     * @return 用户列表
     */
    public List<UserProfileVO> listAllUsers() {
        List<SyUser> users = syUserService.listAllUsers();
        return UserAssembler.toUserProfileVOList(users, buildTenantNameMap(users));
    }

    /**
     * 按条件分页查询用户。
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    public PageInfo<UserProfileVO> pageQueryUsers(UserPageQueryRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        List<SyUser> syUsers = syUserService.pageQueryUsers(
                normalize(request.getUsername()),
                normalize(request.getNickname()),
                normalize(request.getPhone()),
                normalize(request.getEmail()),
                normalizeStatus(request.getStatus())
        );

        PageInfo<SyUser> sourcePageInfo = new PageInfo<>(syUsers);
        PageInfo<UserProfileVO> targetPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(sourcePageInfo, targetPageInfo, "list");
        targetPageInfo.setList(UserAssembler.toUserProfileVOList(sourcePageInfo.getList(), buildTenantNameMap(sourcePageInfo.getList())));
        return targetPageInfo;
    }

    /**
     * 用户统计。
     *
     * @return 统计结果
     */
    public UserStatisticsVO userStatistics() {
        UserStatisticsVO statisticsVO = new UserStatisticsVO();
        statisticsVO.setTotalCount(syUserService.countAllUsers());
        return statisticsVO;
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

    private void validateTenant(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        if (syTenantService.getDetailById(tenantId) == null) {
            throw BusinessExceptions.notFound("租户不存在");
        }
    }

    private Map<Long, String> buildTenantNameMap(List<SyUser> users) {
        List<Long> tenantIds = users.stream()
                .map(SyUser::getTenantId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (tenantIds.isEmpty()) {
            return Map.of();
        }
        return syTenantService.listByIds(tenantIds).stream()
                .collect(Collectors.toMap(SyTenant::getId, SyTenant::getTenantName, (left, right) -> left));
    }

    private String resolveTenantName(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        SyTenant tenant = syTenantService.getDetailById(tenantId);
        return tenant == null ? null : tenant.getTenantName();
    }

    private boolean sameUser(Long currentUserId, Long targetUserId) {
        return currentUserId != null && currentUserId.equals(targetUserId);
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return null;
        }
        UserStatusEnum.fromVal(status);
        return status;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
