package com.spring.ai.user.application.manager;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.spring.ai.common.enums.ErrorCodeEnum;
import com.spring.ai.common.enums.user.UserStatusEnum;
import com.spring.ai.common.exception.BusinessException;
import com.spring.ai.common.repository.enitiy.SyUser;
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
        initializeDefaultTenant(user);
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
        SyUser user = requireUser(userId);
        validateUnique(userId, user.getUsername(), request.getPhone(), request.getEmail());
        UserAssembler.mergeForUpdate(user, request);
        syUserService.updateById(user);
    }

    /**
     * 查询用户信息详情。
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
        targetPageInfo.setList(UserAssembler.toUserProfileVOList(sourcePageInfo.getList()));
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

    /**
     * 初始化默认租户。
     *
     * <p>租户和用户不是同一个概念：
     * 租户代表数据归属边界，未来一个租户下可以有多个用户；
     * 用户代表登录身份、操作者和资源拥有者。
     *
     * <p>当前项目还没有独立的租户管理入口，因此在没有显式传入 tenantId 时，
     * 先为该用户初始化一个默认租户编号。
     * 当前阶段直接使用用户主键作为默认租户编号，只是“租户初始化策略”，
     * 不是把租户和用户建模成同一概念。
     *
     * <p>这样后续接入真正的企业/组织租户体系时，只需要替换这里的租户生成逻辑，
     * Agent、Session、Task 这些按租户隔离的查询都不需要推倒重来。</p>
     *
     * @param user 用户实体
     */
    private void initializeDefaultTenant(SyUser user) {
        if (user == null || user.getTenantId() != null) {
            return;
        }
        user.setTenantId(user.getId());
        syUserService.updateById(user);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
