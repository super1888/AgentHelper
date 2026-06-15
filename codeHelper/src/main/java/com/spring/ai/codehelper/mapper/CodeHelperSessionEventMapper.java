package com.spring.ai.codehelper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionEventRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 代码助手会话事件 Mapper。
 */
@Mapper
public interface CodeHelperSessionEventMapper extends BaseMapper<CodeHelperSessionEventRecord> {
}
