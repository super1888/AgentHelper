package com.spring.ai.user.application.assmbler;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.utils.CommonTextUtils;
import com.spring.ai.user.domain.request.UserCreateRequest;
import com.spring.ai.user.domain.request.UserUpdateRequest;
import com.spring.ai.user.domain.vo.UserProfileVO;
import com.spring.ai.user.domain.vo.UserTokenVO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * 用户模块对象转换器。
 */
public final class UserAssembler {

    private UserAssembler() {
    }

    /**
     * 将用户实体转换为用户展示对象。
     *
     * @param user 用户实体
     * @param tenantName 租户名称
     * @return 用户展示对象
     */
    public static UserProfileVO toUserProfileVO(SyUser user, String tenantName) {
        if (user == null) {
            return null;
        }
        UserProfileVO profileVO = new UserProfileVO();
        profileVO.setId(user.getId());
        profileVO.setUsername(user.getUsername());
        profileVO.setNickname(user.getNickname());
        profileVO.setPhone(user.getPhone());
        profileVO.setEmail(user.getEmail());
        profileVO.setStatus(user.getStatus());
        profileVO.setTenantId(user.getTenantId());
        profileVO.setTenantName(tenantName);
        return profileVO;
    }

    /**
     * 将用户实体列表转换为用户展示对象列表。
     *
     * @param users 用户实体列表
     * @param tenantNameMap 租户名称映射
     * @return 用户展示对象列表
     */
    public static List<UserProfileVO> toUserProfileVOList(List<SyUser> users, Map<Long, String> tenantNameMap) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        return users.stream()
                .map(user -> toUserProfileVO(user, tenantNameMap == null ? null : tenantNameMap.get(user.getTenantId())))
                .collect(Collectors.toList());
    }

    /**
     * 将新增请求转换为用户实体。
     *
     * @param request 新增用户请求
     * @param passwordHash 加密后的密码
     * @return 用户实体
     */
    public static SyUser toCreateEntity(UserCreateRequest request, String passwordHash) {
        SyUser entity = new SyUser();
        entity.setUsername(trim(request.getUsername()));
        entity.setNickname(resolveNickname(request.getNickname(), request.getUsername()));
        entity.setPhone(trimToNull(request.getPhone()));
        entity.setEmail(trimToNull(request.getEmail()));
        entity.setPasswordHash(passwordHash);
        entity.setStatus(request.getStatus());
        entity.setTenantId(request.getTenantId());
        return entity;
    }

    /**
     * 将编辑请求内容合并到原用户实体。
     *
     * @param user 原用户实体
     * @param request 编辑请求
     */
    public static void mergeForUpdate(SyUser user, UserUpdateRequest request) {
        user.setNickname(resolveNickname(request.getNickname(), user.getUsername()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setStatus(request.getStatus());
        user.setTenantId(request.getTenantId());
    }

    /**
     * 构建登录成功后的 token 信息。
     *
     * @param userId 登录用户 ID
     * @return token 展示对象
     */
    public static UserTokenVO toUserTokenVO(Long userId) {
        String tokenName = SaManager.getConfig().getTokenName();
        String tokenPrefix = SaManager.getConfig().getTokenPrefix();
        String tokenValue = StpUtil.getTokenValue();
        String authorizationValue = StringUtils.hasText(tokenPrefix) ? tokenPrefix + " " + tokenValue : tokenValue;

        UserTokenVO tokenVO = new UserTokenVO();
        tokenVO.setTokenName(tokenName);
        tokenVO.setTokenPrefix(tokenPrefix);
        tokenVO.setTokenValue(tokenValue);
        tokenVO.setAuthorizationValue(authorizationValue);
        tokenVO.setExpiresIn(StpUtil.getTokenTimeout());
        tokenVO.setLoginId(userId);
        return tokenVO;
    }

    private static String resolveNickname(String nickname, String username) {
        String normalizedNickname = CommonTextUtils.trimToNull(nickname);
        return StringUtils.hasText(normalizedNickname) ? normalizedNickname : trim(username);
    }

    private static String trimToNull(String value) {
        return CommonTextUtils.trimToNull(value);
    }

    private static String trim(String value) {
        return CommonTextUtils.trim(value);
    }
}
