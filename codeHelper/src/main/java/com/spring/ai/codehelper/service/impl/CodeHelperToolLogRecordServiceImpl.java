package com.spring.ai.codehelper.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.codehelper.domain.entity.CodeHelperToolLogRecord;
import com.spring.ai.codehelper.mapper.CodeHelperToolLogMapper;
import com.spring.ai.codehelper.service.CodeHelperToolLogRecordService;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 代码助手工具日志 Service 实现。
 */
@Service
public class CodeHelperToolLogRecordServiceImpl
        extends ServiceImpl<CodeHelperToolLogMapper, CodeHelperToolLogRecord>
        implements CodeHelperToolLogRecordService {

    @Override
    public List<CodeHelperToolLogRecord> listBySessionCode(String sessionCode, Long tenantId) {
        return list(Wrappers.lambdaQuery(CodeHelperToolLogRecord.class)
                .eq(CodeHelperToolLogRecord::getSessionCode, sessionCode)
                .eq(CodeHelperToolLogRecord::getTenantId, tenantId))
                .stream()
                .sorted(Comparator.comparing(CodeHelperToolLogRecord::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public List<CodeHelperToolLogRecord> listByTenantId(Long tenantId) {
        return list(Wrappers.lambdaQuery(CodeHelperToolLogRecord.class)
                .eq(CodeHelperToolLogRecord::getTenantId, tenantId))
                .stream()
                .sorted(Comparator.comparing(CodeHelperToolLogRecord::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }
}
