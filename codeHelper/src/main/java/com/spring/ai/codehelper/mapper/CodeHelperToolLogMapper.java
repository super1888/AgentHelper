package com.spring.ai.codehelper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spring.ai.codehelper.domain.entity.CodeHelperToolLogRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 代码助手工具日志 Mapper。
 */
@Mapper
public interface CodeHelperToolLogMapper extends BaseMapper<CodeHelperToolLogRecord> {
}
