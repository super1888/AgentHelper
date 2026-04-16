package com.spring.ai.common.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.ai.common.repository.enitiy.Agent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}
