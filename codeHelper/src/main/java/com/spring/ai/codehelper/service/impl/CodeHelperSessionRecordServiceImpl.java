package com.spring.ai.codehelper.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionRecord;
import com.spring.ai.codehelper.mapper.CodeHelperSessionMapper;
import com.spring.ai.codehelper.service.CodeHelperSessionRecordService;
import com.spring.ai.common.constants.SqlConstants;
import org.springframework.stereotype.Service;

/**
 * 代码助手会话 Service 实现。
 */
@Service
public class CodeHelperSessionRecordServiceImpl
        extends ServiceImpl<CodeHelperSessionMapper, CodeHelperSessionRecord>
        implements CodeHelperSessionRecordService {

    @Override
    public CodeHelperSessionRecord getBySessionCode(String sessionCode, Long tenantId) {
        return getOne(Wrappers.lambdaQuery(CodeHelperSessionRecord.class)
                .eq(CodeHelperSessionRecord::getSessionCode, sessionCode)
                .eq(CodeHelperSessionRecord::getTenantId, tenantId)
                .last(SqlConstants.LIMIT_ONE));
    }
}
