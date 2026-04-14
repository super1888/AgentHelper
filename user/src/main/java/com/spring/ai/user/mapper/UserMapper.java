package com.spring.ai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.ai.user.domain.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 * 当前先提供基础 CRUD 入口，后续再补充自定义 SQL 和用户查询能力。
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
