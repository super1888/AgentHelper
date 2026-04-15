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
 * 用户管理应用管理器
 */
@Component
public class UserApplicationManager {

    @Resource
    private SyUserService syUserService;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 用户注册
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
     * 新增用户
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
     * 删除用户
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
     * 编辑用户信息
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
     * 查询用户信息明细
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    public UserProfileVO getUserDetail(Long userId) {
        return UserAssembler.toUserProfileVO(requireUser(userId));
    }

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    public List<UserProfileVO> listAllUsers() {
        return UserAssembler.toUserProfileVOList(syUserService.listAllUsers());
    }

    /**
     * 按条件分页查询用户 说明： 1. 默认第一页，每页 20 条 2. 排序方式：修改时间倒序 3. 查询条件与 statistics 保持一致
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
     * 用户统计 说明： 1. 统计条件与 pageQuery 保持一致 2. totalCount / tenantCount 按当前筛选条件统计 3. enabledCount / disabledCount 复用相同条件，仅覆盖状态值
     *
     * @return 统计结果
     */
    public UserStatisticsVO userStatistics() {

        UserStatisticsVO statisticsVO = new UserStatisticsVO();
        statisticsVO.setTotalCount(syUserService.countUsers(null, null, null, null, null));
        statisticsVO.setEnabledCount(syUserService.countUsers(null, null, null, null, UserStatusEnum.ENABLE.getCode()));
        statisticsVO.setDisabledCount(statisticsVO.getTotalCount() - statisticsVO.getEnabledCount());
        statisticsVO.setTenantCount(statisticsVO.getTotalCount());
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


    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
