package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.constants.SqlConstants;
import com.spring.ai.common.repository.dao.SyUserMapper;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyUserService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
                .orderByDesc(SyUser::getCreateTime)
                .orderByDesc(SyUser::getId));
    }

    @Override
    public boolean deleteByUserId(Long userId) {
        return removeById(userId);
    }
}