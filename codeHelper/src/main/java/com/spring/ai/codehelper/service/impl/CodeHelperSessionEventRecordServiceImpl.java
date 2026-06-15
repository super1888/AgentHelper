package com.spring.ai.codehelper.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.codehelper.domain.entity.CodeHelperSessionEventRecord;
import com.spring.ai.codehelper.mapper.CodeHelperSessionEventMapper;
import com.spring.ai.codehelper.service.CodeHelperSessionEventRecordService;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 代码助手会话事件 Service 实现。
 */
@Service
public class CodeHelperSessionEventRecordServiceImpl
        extends ServiceImpl<CodeHelperSessionEventMapper, CodeHelperSessionEventRecord>
        implements CodeHelperSessionEventRecordService {

    @Override
    public List<CodeHelperSessionEventRecord> listBySessionCode(String sessionCode, Long tenantId) {
        return list(Wrappers.lambdaQuery(CodeHelperSessionEventRecord.class)
                .eq(CodeHelperSessionEventRecord::getSessionCode, sessionCode)
                .eq(CodeHelperSessionEventRecord::getTenantId, tenantId))
                .stream()
                .sorted(Comparator.comparing(CodeHelperSessionEventRecord::getEventSequence))
                .toList();
    }

    @Override
    public Long nextSequence(String sessionCode, Long tenantId) {
        return listBySessionCode(sessionCode, tenantId).stream()
                .map(CodeHelperSessionEventRecord::getEventSequence)
                .filter(sequence -> sequence != null)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }
}
