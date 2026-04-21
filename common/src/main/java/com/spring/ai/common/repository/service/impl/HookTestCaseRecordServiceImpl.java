package com.spring.ai.common.repository.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spring.ai.common.repository.dao.HookTestCaseRecordMapper;
import com.spring.ai.common.repository.enitiy.HookTestCaseRecord;
import com.spring.ai.common.repository.service.HookTestCaseRecordService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 文件用途：Hook 测试用例服务实现
 */
@Service
public class HookTestCaseRecordServiceImpl extends ServiceImpl<HookTestCaseRecordMapper, HookTestCaseRecord>
        implements HookTestCaseRecordService {

    @Override
    public List<HookTestCaseRecord> listByHookId(Long hookId, Long tenantId) {
        return list(Wrappers.lambdaQuery(HookTestCaseRecord.class)
                .eq(HookTestCaseRecord::getTenantId, tenantId)
                .eq(HookTestCaseRecord::getHookId, hookId)
                .orderByDesc(HookTestCaseRecord::getUpdateTime)
                .orderByDesc(HookTestCaseRecord::getId));
    }
}
