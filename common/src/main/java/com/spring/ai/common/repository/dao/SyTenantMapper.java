package com.spring.ai.common.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.ai.common.repository.enitiy.SyTenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户数据访问接口。
 */
@Mapper
public interface SyTenantMapper extends BaseMapper<SyTenant> {
}
