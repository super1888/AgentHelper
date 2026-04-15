package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.SyUserMapper;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyUserService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户仓储服务实现
 */
@Service
public class SyUserServiceImpl extends ServiceImpl<SyUserMapper, SyUser> implements SyUserService {

    @Override
    public SyUser getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(SyUser.class)
                .eq(SyUser::getUsername, username)
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public SyUser getByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(SyUser.class)
                .eq(SyUser::getPhone, phone)
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public SyUser getByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return getOne(Wrappers.lambdaQuery(SyUser.class)
                .eq(SyUser::getEmail, email)
                .last(SqlConstants.LIMIT_ONE));
    }

    @Override
    public SyUser getDetailById(Long userId) {
        return getById(userId);
    }

    @Override
    public List<SyUser> listAllUsers() {
        return list(Wrappers.lambdaQuery(SyUser.class)
                .orderByDesc(SyUser::getUpdateTime)
                .orderByDesc(SyUser::getId));
    }

    /**
     * 统一复用分页查询条件，排序按修改时间倒序
     */
    @Override
    public List<SyUser> pageQueryUsers(String username, String nickname, String phone, String email, Integer status) {
        return list(buildLambdaQuery(username, nickname, phone, email, status));
    }

    /**
     * 统计系统总用户数，不接收筛选条件
     */
    @Override
    public long countAllUsers() {
        return count();
    }

    @Override
    public boolean deleteByUserId(Long userId) {
        return removeById(userId);
    }

    private LambdaQueryWrapper<SyUser> buildLambdaQuery(
            String username,
            String nickname,
            String phone,
            String email,
            Integer status
    ) {
        LambdaQueryWrapper<SyUser> queryWrapper = Wrappers.lambdaQuery(SyUser.class)
                .orderByDesc(SyUser::getUpdateTime)
                .orderByDesc(SyUser::getId);

        if (StringUtils.hasText(username)) {
            queryWrapper.like(SyUser::getUsername, username.trim());
        }
        if (StringUtils.hasText(nickname)) {
            queryWrapper.like(SyUser::getNickname, nickname.trim());
        }
        if (StringUtils.hasText(phone)) {
            queryWrapper.like(SyUser::getPhone, phone.trim());
        }
        if (StringUtils.hasText(email)) {
            queryWrapper.like(SyUser::getEmail, email.trim());
        }
        if (status != null) {
            queryWrapper.eq(SyUser::getStatus, status);
        }
        return queryWrapper;
    }
}
