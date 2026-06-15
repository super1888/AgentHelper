package com.spring.ai.codehelper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 代码助手会话 Mapper。
 */
@Mapper
public interface CodeHelperSessionMapper extends BaseMapper<CodeHelperSessionRecord> {
}
