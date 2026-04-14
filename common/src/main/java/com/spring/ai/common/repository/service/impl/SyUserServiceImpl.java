package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.SyUserMapper;
import com.spring.ai.common.repository.enitiy.SyUser;
import com.spring.ai.common.repository.service.SyUserService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 用户仓储服务实现。
 */
@Service
public class SyUserServiceImpl extends ServiceImpl<SyUserMapper, SyUser> implements SyUserService {

    @Override
    public SyUser getByUsername(String username) {
        return baseMapper.selectByUsername(username);
    }

    @Override
    public SyUser getByPhone(String phone) {
        return baseMapper.selectByPhone(phone);
    }

    @Override
    public SyUser getByEmail(String email) {
        return baseMapper.selectByEmail(email);
    }

    @Override
    public SyUser getDetailById(Long userId) {
        return baseMapper.selectDetailById(userId);
    }

    @Override
    public List<SyUser> listAllUsers() {
        return baseMapper.selectAllUsers();
    }

    @Override
    public boolean deleteByUserId(Long userId) {
        return baseMapper.deleteByUserId(userId) > 0;
    }
}
