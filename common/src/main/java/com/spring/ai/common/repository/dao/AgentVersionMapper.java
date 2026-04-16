package com.spring.ai.common.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.ai.common.repository.enitiy.AgentVersion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentVersionMapper extends BaseMapper<AgentVersion> {
}
